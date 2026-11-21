package tn.iset.smartrecruit.cv_service.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tn.iset.smartrecruit.cv_service.model.Profil;
import tn.iset.smartrecruit.cv_service.model.QuestionEntretien;
import tn.iset.smartrecruit.cv_service.model.SessionPreparation;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

@Service
public class InterviewAiClient {
    private static final Logger log = LoggerFactory.getLogger(InterviewAiClient.class);
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1";
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String primaryModel;
    private final String fallbackModels;

    @Autowired
    public InterviewAiClient(RestClient.Builder builder, ObjectMapper objectMapper,
                             @Value("${app.groq.api-key:}") String apiKey,
                             @Value("${app.groq.model:}") String primaryModel,
                             @Value("${app.groq.fallback-models:}") String fallbackModels) {
        this(createClient(builder), objectMapper, apiKey, primaryModel, fallbackModels);
    }

    InterviewAiClient(RestClient client, ObjectMapper objectMapper, String apiKey,
                      String primaryModel, String fallbackModels) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.primaryModel = primaryModel;
        this.fallbackModels = fallbackModels;
    }

    public List<GeneratedQuestion> generateQuestions(ProfileContext profil, OfferContext offre) {
        String prompt = questionPrompt(profil, offre);
        QuestionsResponse response = generateJson(prompt, QuestionsResponse.class,
                InterviewAiClient::normalizeQuestionTypes, InterviewAiClient::validQuestions);
        return response.questions();
    }

    public QuestionEntretien.Evaluation evaluate(String type, String question, String answer) {
        String prompt = """
                Evaluate the candidate's answer against the exact interview question. Consider relevance,
                technical correctness and depth appropriate to the question type. Do not reward unsupported claims.
                Identify specific missing or incorrect points, and give constructive advice. Score from 0 to 100
                based on this answer; do not use a fixed or default score. Return only JSON:
                {"score":0,"pertinence":"...","exactitude":"...","pointsManquants":["..."],"retour":"..."}.
                Write feedback in French.
                Question type: %s
                Question: %s
                Candidate answer: %s
                """.formatted(type, question, answer);
        AnswerEvaluation response = generateJson(prompt, AnswerEvaluation.class, Function.identity(),
                InterviewAiClient::validAnswerEvaluation);
        return new QuestionEntretien.Evaluation(response.score(), response.pertinence(),
                response.exactitude(), response.pointsManquants(), response.retour());
    }

    public SessionPreparation.EvaluationFinale evaluateSession(List<AnsweredQuestion> answers) {
        String prompt = """
                Review the candidate's actual interview questions, answers, and per-answer evaluations.
                Summarize evidence-based strengths, weaknesses, actionable areas to improve, and general interview
                advice. Do not introduce claims unsupported by the answers. Return only JSON:
                {"forces":["..."],"faiblesses":["..."],"axesAmelioration":["..."],"conseils":"..."}.
                Write feedback in French. Interview responses:
                %s
                """.formatted(toJson(answers));
        FinalEvaluation response = generateJson(prompt, FinalEvaluation.class, Function.identity(),
                InterviewAiClient::validFinalEvaluation);
        return new SessionPreparation.EvaluationFinale(response.forces(), response.faiblesses(),
                response.axesAmelioration(), response.conseils());
    }

    private String questionPrompt(ProfileContext profil, OfferContext offre) {
        return """
                You are an experienced interviewer. Generate 8 varied interview questions dynamically from
                the candidate profile below. The competencesValidees field contains the only explicitly validated skills.
                Never treat other profile text as a validated skill, and never invent a skill, project, education, or experience.
                Every question, including general and behavioral questions, must be specific to at least one fact in this
                candidate's profile; do not use generic or prewritten interview questions.
                Use technical questions grounded in validated skills and, when available, experience-based questions grounded
                in the candidate's actual work, projects, education, languages, or profile sections. If some information is
                absent, do not fabricate it; ground every question in information that is present.
                When a job offer is provided, also connect every question to its role, description, or required skills, without
                claiming the candidate already has skills that are not validated or stated in their actual experience.
                Avoid semantic repetition within this set. Write the questions in French.
                Return only a JSON object with a top-level "questions" array. Each array item must contain "type" (exactly one
                of TECHNIQUE, EXPERIENCE, or GENERALE) and "question" (one non-empty question). Do not combine type labels.
                Candidate profile JSON:
                %s
                Job offer JSON:
                %s
                """.formatted(toJson(profil), toJson(offre));
    }

    private <T> T generateJson(String prompt, Class<T> responseType,
                               Function<T, T> normalize, Predicate<T> validResponse) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La clé API Groq n’est pas configurée pour la préparation aux entretiens.");
        }
        List<String> models = configuredModels();
        if (models.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Aucun modèle Groq n’est configuré pour la préparation aux entretiens.");
        }

        int lastFailureStatus = 0;
        boolean invalidResponse = false;
        for (String model : models) {
            try {
                ChatCompletionResponse completion = client.post()
                        .uri(CHAT_COMPLETIONS_PATH)
                        .header("Authorization", "Bearer " + apiKey)
                        .body(new ChatCompletionRequest(model,
                                List.of(new ChatMessage("user", prompt)),
                                new ResponseFormat("json_object"), 0.2))
                        .retrieve()
                        .body(ChatCompletionResponse.class);
                String content = completionContent(completion);
                if (content == null || content.isBlank()) {
                    log.warn("Groq interview response was empty for model={}", model);
                    invalidResponse = true;
                    lastFailureStatus = 0;
                    continue;
                }

                T parsed;
                try {
                    parsed = objectMapper.readValue(stripJsonFence(content), responseType);
                } catch (JsonProcessingException error) {
                    log.warn("Groq interview response was invalid JSON for model={} exception={}",
                            model, error.getClass().getSimpleName());
                    invalidResponse = true;
                    lastFailureStatus = 0;
                    continue;
                }
                T normalized = normalize.apply(parsed);
                if (!validResponse.test(normalized)) {
                    log.warn("Groq interview response failed validation for model={}", model);
                    invalidResponse = true;
                    lastFailureStatus = 0;
                    continue;
                }
                return normalized;
            } catch (RestClientResponseException error) {
                int status = error.getStatusCode().value();
                log.warn("Groq interview request failed for model={} status={}", model, status);
                if (status == 401 || status == 403) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "La clé API ou les permissions Groq sont invalides.");
                }
                lastFailureStatus = status;
            } catch (RestClientException error) {
                log.warn("Groq interview request failed for model={} exception={}",
                        model, error.getClass().getSimpleName());
                lastFailureStatus = 503;
            }
        }

        if (lastFailureStatus == 429) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Tous les modèles Groq configurés ont atteint leur limite d’utilisation.");
        }
        if (lastFailureStatus == 404 || lastFailureStatus == 408 || lastFailureStatus >= 500) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Tous les modèles Groq configurés sont indisponibles.");
        }
        if (lastFailureStatus >= 400) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Groq a refusé la requête avec tous les modèles configurés.");
        }
        if (invalidResponse) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Groq n’a renvoyé aucune réponse JSON valide avec les modèles configurés.");
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Aucun modèle Groq n’a pu traiter la demande.");
    }

    private static RestClient createClient(RestClient.Builder builder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(45));
        return builder.baseUrl(GROQ_API_URL).requestFactory(requestFactory).build();
    }

    private List<String> configuredModels() {
        if (primaryModel == null || primaryModel.isBlank()) {
            return List.of();
        }
        List<String> models = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        addModel(models, seen, primaryModel);
        if (fallbackModels != null) {
            for (String model : fallbackModels.split(",")) {
                addModel(models, seen, model);
            }
        }
        return models;
    }

    private static void addModel(List<String> models, Set<String> seen, String model) {
        String trimmed = model == null ? "" : model.trim();
        if (!trimmed.isEmpty() && seen.add(trimmed)) {
            models.add(trimmed);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible de préparer le contexte de l’entretien.");
        }
    }

    private static String completionContent(ChatCompletionResponse completion) {
        if (completion == null || completion.choices() == null || completion.choices().isEmpty()
                || completion.choices().getFirst() == null
                || completion.choices().getFirst().message() == null) {
            return null;
        }
        return completion.choices().getFirst().message().content();
    }

    private static String stripJsonFence(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }

    private static QuestionsResponse normalizeQuestionTypes(QuestionsResponse response) {
        if (response == null || response.questions() == null) {
            return response;
        }
        List<GeneratedQuestion> normalized = response.questions().stream()
                .map(question -> {
                    if (question == null || question.type() == null) {
                        return question;
                    }
                    String type = question.type().trim().toUpperCase(Locale.ROOT);
                    if (type.equals("TECHNICAL")) type = "TECHNIQUE";
                    if (type.equals("GENERAL")) type = "GENERALE";
                    return new GeneratedQuestion(type, question.question());
                })
                .toList();
        return new QuestionsResponse(normalized);
    }

    private static boolean validQuestions(QuestionsResponse response) {
        if (response == null || response.questions() == null
                || response.questions().size() < 3 || response.questions().size() > 15) {
            return false;
        }
        Set<String> normalizedQuestions = new HashSet<>();
        for (GeneratedQuestion question : response.questions()) {
            if (question == null || question.type() == null
                    || !List.of("TECHNIQUE", "EXPERIENCE", "GENERALE").contains(question.type())
                    || question.question() == null || question.question().isBlank()) {
                return false;
            }
            String normalized = question.question().toLowerCase(Locale.ROOT)
                    .replaceAll("(?U)\\W+", " ").trim();
            if (!normalizedQuestions.add(normalized)) {
                return false;
            }
        }
        return true;
    }

    private static boolean validAnswerEvaluation(AnswerEvaluation response) {
        return response != null && response.score() >= 0 && response.score() <= 100
                && response.pertinence() != null && response.exactitude() != null
                && response.pointsManquants() != null && response.retour() != null;
    }

    private static boolean validFinalEvaluation(FinalEvaluation response) {
        return response != null && response.forces() != null && response.faiblesses() != null
                && response.axesAmelioration() != null && response.conseils() != null;
    }

    public record OfferContext(String id, String titre, String description, List<String> competences) {}
    public record GeneratedQuestion(String type, String question) {}
    public record ProfileContext(String titre, String resume,
                                 List<Profil.Competence> competencesValidees,
                                 List<Profil.Experience> experiences, List<Profil.Formation> formations,
                                 List<Profil.Langue> langues, List<Profil.SectionPerso> sections) {}
    public record QuestionsResponse(List<GeneratedQuestion> questions) {}
    public record AnswerEvaluation(int score, String pertinence, String exactitude,
                                   List<String> pointsManquants, String retour) {}
    public record AnsweredQuestion(String type, String question, String reponse,
                                   QuestionEntretien.Evaluation evaluation) {}
    public record FinalEvaluation(List<String> forces, List<String> faiblesses,
                                  List<String> axesAmelioration, String conseils) {}

    private record ChatCompletionRequest(String model, List<ChatMessage> messages,
                                         @JsonProperty("response_format") ResponseFormat responseFormat,
                                         double temperature) {}
    private record ChatMessage(String role, String content) {}
    private record ResponseFormat(String type) {}
    private record ChatCompletionResponse(List<ChatChoice> choices) {}
    private record ChatChoice(ChatResponseMessage message) {}
    private record ChatResponseMessage(String content) {}
}
