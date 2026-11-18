package tn.iset.smartrecruit.cv_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tn.iset.smartrecruit.cv_service.model.Profil;
import tn.iset.smartrecruit.cv_service.model.QuestionEntretien;
import tn.iset.smartrecruit.cv_service.model.SessionPreparation;

import java.util.List;

@Service
public class InterviewAiClient {
    private final RestClient client;

    public InterviewAiClient(RestClient.Builder builder,
                             @Value("${app.ocr-url:http://localhost:8083}") String aiServiceUrl) {
        this.client = builder.baseUrl(aiServiceUrl).build();
    }

    public List<GeneratedQuestion> generateQuestions(ProfileContext profil, OfferContext offre) {
        QuestionsResponse response = post("/interview/questions",
                new QuestionsRequest(profil, offre), QuestionsResponse.class);
        if (response == null || response.questions() == null || response.questions().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Le service IA n’a généré aucune question.");
        }
        return response.questions();
    }

    public QuestionEntretien.Evaluation evaluate(String type, String question, String answer) {
        AnswerEvaluation response = post("/interview/evaluate",
                new AnswerRequest(type, question, answer), AnswerEvaluation.class);
        if (response == null || response.score() < 0 || response.score() > 100
                || response.pertinence() == null || response.exactitude() == null
                || response.pointsManquants() == null || response.retour() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Évaluation IA invalide.");
        }
        return new QuestionEntretien.Evaluation(response.score(), response.pertinence(),
                response.exactitude(), response.pointsManquants(), response.retour());
    }

    public SessionPreparation.EvaluationFinale evaluateSession(List<AnsweredQuestion> answers) {
        FinalEvaluation response = post("/interview/final-evaluation",
                new FinalEvaluationRequest(answers), FinalEvaluation.class);
        if (response == null || response.forces() == null || response.faiblesses() == null
                || response.axesAmelioration() == null || response.conseils() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Bilan IA invalide.");
        }
        return new SessionPreparation.EvaluationFinale(response.forces(), response.faiblesses(),
                response.axesAmelioration(), response.conseils());
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        try {
            return client.post().uri(path).body(body).retrieve().body(responseType);
        } catch (RestClientException e) {
            HttpStatus status = !(e instanceof RestClientResponseException response)
                    || response.getStatusCode().is5xxServerError()
                    ? HttpStatus.BAD_GATEWAY : HttpStatus.SERVICE_UNAVAILABLE;
            throw new ResponseStatusException(status, "Le service IA est indisponible. Réessayez plus tard.");
        }
    }

    public record OfferContext(String id, String titre, String description, List<String> competences) {}
    public record GeneratedQuestion(String type, String question) {}
    public record ProfileContext(String titre, String resume,
                                 List<Profil.Competence> competencesValidees,
                                 List<Profil.Experience> experiences, List<Profil.Formation> formations,
                                 List<Profil.Langue> langues, List<Profil.SectionPerso> sections) {}
    public record QuestionsRequest(ProfileContext profil, OfferContext offre) {}
    public record QuestionsResponse(List<GeneratedQuestion> questions) {}
    public record AnswerRequest(String type, String question, String reponse) {}
    public record AnswerEvaluation(int score, String pertinence, String exactitude,
                                   List<String> pointsManquants, String retour) {}
    public record AnsweredQuestion(String type, String question, String reponse,
                                   QuestionEntretien.Evaluation evaluation) {}
    public record FinalEvaluationRequest(List<AnsweredQuestion> reponses) {}
    public record FinalEvaluation(List<String> forces, List<String> faiblesses,
                                  List<String> axesAmelioration, String conseils) {}
}
