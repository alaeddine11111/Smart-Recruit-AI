package tn.iset.smartrecruit.cv_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.client.MockRestServiceServer;
import tn.iset.smartrecruit.cv_service.dto.StartPreparationRequest;
import tn.iset.smartrecruit.cv_service.dto.SubmitPreparationAnswerRequest;
import tn.iset.smartrecruit.cv_service.model.Profil;
import tn.iset.smartrecruit.cv_service.model.QuestionEntretien;
import tn.iset.smartrecruit.cv_service.model.SessionPreparation;
import tn.iset.smartrecruit.cv_service.repository.SessionPreparationRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SessionPreparationServiceTest {
    private final SessionPreparationRepository sessions = mock(SessionPreparationRepository.class);
    private final ProfilService profiles = mock(ProfilService.class);
    private final InterviewAiClient ai = mock(InterviewAiClient.class);
    private final SessionPreparationService service = new SessionPreparationService(
            sessions, profiles, ai, RestClient.builder(), "http://offer-service");
    private final Profil profile = new Profil("profile-1", "candidate-1", "Développeur",
            "Profil Java et services REST", "", "",
            List.of(new Profil.Experience("Développeuse backend", "Example", "2022", "2024",
                    "Développement de services REST Java")),
            List.of(new Profil.Formation("Licence informatique", "ISET", "2019", "2022")),
            List.of(new Profil.Competence("Legacy", "Backend")),
            List.of(new Profil.Competence("Python OCR", "Détectée")),
            List.of(new Profil.Competence("Java", "Backend")),
            List.of(new Profil.Langue("Français", "Courant")),
            List.of(new Profil.SectionPerso("project-1", "Projets", List.of("API REST Java"))),
            true, Instant.now());
    private final List<InterviewAiClient.GeneratedQuestion> generatedQuestions = List.of(
            new InterviewAiClient.GeneratedQuestion("TECHNIQUE", "Comment fonctionne Java ?"),
            new InterviewAiClient.GeneratedQuestion("EXPERIENCE", "Parlez d’un projet réalisé."),
            new InterviewAiClient.GeneratedQuestion("GENERALE", "Quelles sont vos motivations ?"));

    @BeforeEach
    void setUp() {
        when(profiles.get("candidate-1")).thenReturn(profile);
        when(ai.generateQuestions(any(InterviewAiClient.ProfileContext.class), isNull()))
                .thenReturn(generatedQuestions);
        when(sessions.save(any(SessionPreparation.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsProfileBasedGeneralSessionWithGeneratedQuestions() {
        SessionPreparation created = service.start("candidate-1", "Bearer token",
                new StartPreparationRequest(null));

        assertEquals("candidate-1", created.candidatId());
        assertEquals("Entretien général", created.offreTitre());
        assertEquals(3, created.questions().size());
        assertEquals(SessionPreparation.StatutPreparation.EN_COURS, created.statut());
        ArgumentCaptor<InterviewAiClient.ProfileContext> context =
                ArgumentCaptor.forClass(InterviewAiClient.ProfileContext.class);
        verify(ai).generateQuestions(context.capture(), isNull());
        assertEquals("Profil Java et services REST", context.getValue().resume());
        assertEquals("Développeuse backend",
                context.getValue().experiences().getFirst().poste());
        assertEquals("Licence informatique",
                context.getValue().formations().getFirst().diplome());
        assertEquals("Français", context.getValue().langues().getFirst().nom());
        assertEquals("API REST Java", context.getValue().sections().getFirst().elements().getFirst());
        assertEquals(List.of("Java"), context.getValue().competencesValidees().stream()
                .map(Profil.Competence::nom).toList());
        assertFalse(context.getValue().competencesValidees().stream()
                .anyMatch(skill -> skill.nom().equals("Python OCR")));
    }

    @Test
    void requiresValidatedSkillsAndDoesNotPersistASession() {
        Profil withoutValidatedSkills = new Profil(profile.id(), profile.candidatId(), profile.titre(),
                profile.resume(), profile.telephone(), profile.localisation(), profile.experiences(),
                profile.formations(), profile.competences(),
                List.of(new Profil.Competence("Python OCR", "Détectée")), List.of(),
                profile.langues(), profile.sections(), profile.valide(), profile.misAJourLe());
        when(profiles.get("candidate-1")).thenReturn(withoutValidatedSkills);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.start("candidate-1", "Bearer token", new StartPreparationRequest(null)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertTrue(error.getReason().contains("validez au moins une compétence"));
        verify(ai, never()).generateQuestions(any(), any());
        verify(sessions, never()).save(any());
    }

    @Test
    void createsOfferSpecificSessionWithVerifiedOfferContext() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SessionPreparationService offerService = new SessionPreparationService(
                sessions, profiles, ai, builder, "http://offer-service");
        when(ai.generateQuestions(any(InterviewAiClient.ProfileContext.class),
                any(InterviewAiClient.OfferContext.class))).thenReturn(generatedQuestions);
        server.expect(requestTo("http://offer-service/api/offres/offer-1"))
                .andExpect(header("Authorization", "Bearer candidate-token"))
                .andRespond(withSuccess("""
                        {"id":"offer-1","titre":"Développeur Java","description":"Backend Java",
                         "competences":["Java","Spring"]}
                        """, MediaType.APPLICATION_JSON));

        SessionPreparation created = offerService.start("candidate-1", "Bearer candidate-token",
                new StartPreparationRequest("offer-1"));

        assertEquals("offer-1", created.offreId());
        assertEquals("Développeur Java", created.offreTitre());
        verify(ai).generateQuestions(any(InterviewAiClient.ProfileContext.class),
                argThat(offer -> offer != null && offer.competences().contains("Spring")));
        server.verify();
    }

    @Test
    void savesActualAnswerAndAiEvaluation() {
        SessionPreparation session = inProgressSession();
        when(sessions.findByIdAndCandidatId(session.id(), "candidate-1")).thenReturn(Optional.of(session));
        when(ai.evaluate("TECHNIQUE", "Comment fonctionne Java ?", "Ma réponse."))
                .thenReturn(new QuestionEntretien.Evaluation(74, "Pertinente", "Partiellement exacte",
                        List.of("Gestion de la mémoire"), "Précisez le rôle du garbage collector."));

        SessionPreparation updated = service.submitAnswer("candidate-1", session.id(),
                new SubmitPreparationAnswerRequest("q1", " Ma réponse. "));

        assertEquals("Ma réponse.", updated.questions().getFirst().reponse());
        assertEquals(74, updated.questions().getFirst().evaluation().score());
        verify(sessions).save(updated);
    }

    @Test
    void finishUsesMeanOfEvaluationsAndAiFinalReview() {
        SessionPreparation session = answeredSession();
        when(sessions.findByIdAndCandidatId(session.id(), "candidate-1")).thenReturn(Optional.of(session));
        when(ai.evaluateSession(any())).thenReturn(new SessionPreparation.EvaluationFinale(
                List.of("Réponses pertinentes"), List.of("Manque de détails"),
                List.of("Illustrer les réponses"), "Préparez des exemples concrets."));

        SessionPreparation result = service.finish("candidate-1", session.id());

        assertEquals(70, result.scoreGlobal());
        assertEquals(SessionPreparation.StatutPreparation.TERMINEE, result.statut());
        assertNotNull(result.dateFin());
        assertEquals("Préparez des exemples concrets.", result.evaluationFinale().conseils());
    }

    @Test
    void historyIsScopedToCandidate() {
        when(sessions.findByCandidatIdOrderByDateCreationDesc("candidate-1")).thenReturn(List.of(inProgressSession()));

        assertEquals(1, service.history("candidate-1").size());
        verify(sessions).findByCandidatIdOrderByDateCreationDesc("candidate-1");
    }

    @Test
    void candidateCannotReadAnotherCandidatesSession() {
        when(sessions.findByIdAndCandidatId("session-1", "candidate-2")).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.get("candidate-2", "session-1"));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void doesNotPersistSessionWhenQuestionGenerationFails() {
        when(ai.generateQuestions(any(InterviewAiClient.ProfileContext.class), isNull()))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI unavailable"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.start("candidate-1", "Bearer token", new StartPreparationRequest(null)));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        verify(sessions, never()).save(any());
    }

    @Test
    void doesNotPersistAnswerWhenAiEvaluationFails() {
        SessionPreparation session = inProgressSession();
        when(sessions.findByIdAndCandidatId(session.id(), "candidate-1")).thenReturn(Optional.of(session));
        when(ai.evaluate("TECHNIQUE", "Comment fonctionne Java ?", "Ma réponse."))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI unavailable"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.submitAnswer("candidate-1", session.id(),
                        new SubmitPreparationAnswerRequest("q1", "Ma réponse.")));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
        verify(sessions, never()).save(any());
    }

    @Test
    void cannotFinishUntilEveryAnswerHasBeenEvaluated() {
        SessionPreparation session = inProgressSession();
        when(sessions.findByIdAndCandidatId(session.id(), "candidate-1")).thenReturn(Optional.of(session));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.finish("candidate-1", session.id()));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(ai, never()).evaluateSession(any());
    }

    @Test
    void doesNotFinishSessionWhenFinalAiReviewFails() {
        SessionPreparation session = answeredSession();
        when(sessions.findByIdAndCandidatId(session.id(), "candidate-1")).thenReturn(Optional.of(session));
        when(ai.evaluateSession(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI unavailable"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.finish("candidate-1", session.id()));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
        verify(sessions, never()).save(any());
    }

    private SessionPreparation inProgressSession() {
        return new SessionPreparation("session-1", "candidate-1", null, "Entretien général",
                Instant.now(), null, List.of(
                new QuestionEntretien("q1", "TECHNIQUE", "Comment fonctionne Java ?", null, null),
                new QuestionEntretien("q2", "EXPERIENCE", "Parlez d’un projet réalisé.", null, null),
                new QuestionEntretien("q3", "GENERALE", "Quelles sont vos motivations ?", null, null)),
                null, null, SessionPreparation.StatutPreparation.EN_COURS);
    }

    private SessionPreparation answeredSession() {
        QuestionEntretien.Evaluation first = new QuestionEntretien.Evaluation(
                80, "Bonne", "Exacte", List.of(), "Bonne maîtrise.");
        QuestionEntretien.Evaluation second = new QuestionEntretien.Evaluation(
                60, "Partielle", "À approfondir", List.of("Précision"), "Ajoutez un exemple.");
        QuestionEntretien.Evaluation third = new QuestionEntretien.Evaluation(
                70, "Pertinente", "Correcte", List.of(), "Réponse claire.");
        SessionPreparation session = inProgressSession();
        return new SessionPreparation(session.id(), session.candidatId(), session.offreId(), session.offreTitre(),
                session.dateCreation(), null, List.of(
                new QuestionEntretien("q1", "TECHNIQUE", "Comment fonctionne Java ?", "Réponse 1", first),
                new QuestionEntretien("q2", "EXPERIENCE", "Parlez d’un projet réalisé.", "Réponse 2", second),
                new QuestionEntretien("q3", "GENERALE", "Quelles sont vos motivations ?", "Réponse 3", third)),
                null, null, SessionPreparation.StatutPreparation.EN_COURS);
    }
}
