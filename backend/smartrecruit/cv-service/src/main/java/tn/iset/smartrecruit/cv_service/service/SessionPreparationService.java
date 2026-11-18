package tn.iset.smartrecruit.cv_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tn.iset.smartrecruit.cv_service.dto.StartPreparationRequest;
import tn.iset.smartrecruit.cv_service.dto.SubmitPreparationAnswerRequest;
import tn.iset.smartrecruit.cv_service.model.Profil;
import tn.iset.smartrecruit.cv_service.model.QuestionEntretien;
import tn.iset.smartrecruit.cv_service.model.SessionPreparation;
import tn.iset.smartrecruit.cv_service.model.SessionPreparation.StatutPreparation;
import tn.iset.smartrecruit.cv_service.repository.SessionPreparationRepository;
import tn.iset.smartrecruit.cv_service.service.InterviewAiClient.AnsweredQuestion;
import tn.iset.smartrecruit.cv_service.service.InterviewAiClient.GeneratedQuestion;
import tn.iset.smartrecruit.cv_service.service.InterviewAiClient.OfferContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SessionPreparationService {
    private final SessionPreparationRepository sessions;
    private final ProfilService profils;
    private final InterviewAiClient ai;
    private final RestClient offerService;

    public SessionPreparationService(SessionPreparationRepository sessions, ProfilService profils,
                                     InterviewAiClient ai, RestClient.Builder builder,
                                     @Value("${app.offer-service-url:http://localhost:8084}") String offerServiceUrl) {
        this.sessions = sessions;
        this.profils = profils;
        this.ai = ai;
        this.offerService = builder.baseUrl(offerServiceUrl).build();
    }

    public SessionPreparation start(String candidatId, String authorization, StartPreparationRequest request) {
        Profil profile = profils.get(candidatId);
        List<Profil.Competence> validatedSkills = profile == null
                ? List.of() : nonNull(profile.competencesValidees()).stream()
                .filter(skill -> skill != null && skill.nom() != null && !skill.nom().isBlank())
                .toList();
        if (validatedSkills.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Complétez votre profil et validez au moins une compétence avant de démarrer la préparation.");
        }
        OfferContext offer = request.offreId() == null || request.offreId().isBlank()
                ? null : fetchOffer(request.offreId(), authorization);
        InterviewAiClient.ProfileContext profileContext = new InterviewAiClient.ProfileContext(
                profile.titre() == null ? "" : profile.titre(),
                profile.resume() == null ? "" : profile.resume(),
                validatedSkills, nonNull(profile.experiences()), nonNull(profile.formations()),
                nonNull(profile.langues()), nonNull(profile.sections()));
        List<GeneratedQuestion> generated = ai.generateQuestions(profileContext, offer);
        if (generated.size() < 3 || generated.size() > 15
                || generated.stream().anyMatch(q -> q == null || q.type() == null
                || !List.of("TECHNIQUE", "EXPERIENCE", "GENERALE").contains(q.type())
                || q.question() == null || q.question().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Le service IA a renvoyé un jeu de questions invalide.");
        }
        List<QuestionEntretien> questions = generated.stream()
                .map(q -> new QuestionEntretien(UUID.randomUUID().toString(), q.type(), q.question(), null, null))
                .toList();
        return sessions.save(new SessionPreparation(null, candidatId,
                offer == null ? null : offer.id(), offer == null ? "Entretien général" : offer.titre(),
                Instant.now(), null, questions, null, null, StatutPreparation.EN_COURS));
    }

    public List<SessionPreparation> history(String candidatId) {
        return sessions.findByCandidatIdOrderByDateCreationDesc(candidatId);
    }

    public SessionPreparation get(String candidatId, String id) {
        return ownedSession(candidatId, id);
    }

    public SessionPreparation submitAnswer(String candidatId, String id, SubmitPreparationAnswerRequest request) {
        SessionPreparation session = ownedSession(candidatId, id);
        ensureInProgress(session);
        List<QuestionEntretien> questions = session.questions().stream().map(question -> {
            if (!question.id().equals(request.questionId())) return question;
            QuestionEntretien.Evaluation evaluation = ai.evaluate(
                    question.type(), question.question(), request.reponse().trim());
            return new QuestionEntretien(question.id(), question.type(), question.question(),
                    request.reponse().trim(), evaluation);
        }).toList();
        if (questions.stream().noneMatch(q -> q.id().equals(request.questionId()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question introuvable dans cette session.");
        }
        return sessions.save(copy(session, questions, null, null, StatutPreparation.EN_COURS, null));
    }

    public SessionPreparation finish(String candidatId, String id) {
        SessionPreparation session = ownedSession(candidatId, id);
        ensureInProgress(session);
        if (session.questions().stream().anyMatch(q -> q.reponse() == null || q.evaluation() == null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Répondez à toutes les questions avant de terminer.");
        }
        List<AnsweredQuestion> answers = session.questions().stream()
                .map(q -> new AnsweredQuestion(q.type(), q.question(), q.reponse(), q.evaluation())).toList();
        SessionPreparation.EvaluationFinale finalEvaluation = ai.evaluateSession(answers);
        int score = (int) Math.round(session.questions().stream()
                .mapToInt(q -> q.evaluation().score()).average().orElseThrow());
        return sessions.save(copy(session, session.questions(), score, finalEvaluation,
                StatutPreparation.TERMINEE, Instant.now()));
    }

    private OfferContext fetchOffer(String offerId, String authorization) {
        try {
            OfferContext offer = offerService.get().uri("/api/offres/{id}", offerId)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve().body(OfferContext.class);
            if (offer == null || offer.id() == null || offer.titre() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Réponse invalide du service des offres.");
            }
            return new OfferContext(offer.id(), offer.titre(),
                    offer.description() == null ? "" : offer.description(),
                    nonNull(offer.competences()));
        } catch (RestClientException e) {
            HttpStatus status = e instanceof RestClientResponseException response
                    && response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)
                    ? HttpStatus.NOT_FOUND : HttpStatus.BAD_GATEWAY;
            throw new ResponseStatusException(status, "L’offre sélectionnée est introuvable ou indisponible.");
        }
    }

    private SessionPreparation ownedSession(String candidatId, String id) {
        return sessions.findByIdAndCandidatId(id, candidatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session introuvable."));
    }

    private static void ensureInProgress(SessionPreparation session) {
        if (session.statut() != StatutPreparation.EN_COURS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette session est déjà terminée.");
        }
    }

    private static <T> List<T> nonNull(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static SessionPreparation copy(SessionPreparation session, List<QuestionEntretien> questions,
                                           Integer score, SessionPreparation.EvaluationFinale evaluation,
                                           StatutPreparation status, Instant dateFin) {
        return new SessionPreparation(session.id(), session.candidatId(), session.offreId(),
                session.offreTitre(), session.dateCreation(), dateFin, questions, score, evaluation, status);
    }
}
