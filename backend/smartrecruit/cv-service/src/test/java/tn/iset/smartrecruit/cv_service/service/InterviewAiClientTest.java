package tn.iset.smartrecruit.cv_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.client.MockRestServiceServer;
import tn.iset.smartrecruit.cv_service.model.Profil;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class InterviewAiClientTest {
    private static final String API_URL = "https://api.groq.com/openai/v1";
    private static final String API_KEY = "test-groq-key";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void primaryModelGeneratesQuestionsWithProfileAndOfferContext() throws Exception {
        TestClient testClient = client("primary-model", "");
        expectContent(testClient.server(), "primary-model", """
                {"questions":[
                  {"type":"TECHNICAL","question":"Comment utilisez-vous Java ?"},
                  {"type":"EXPERIENCE","question":"Décrivez votre expérience Java."},
                  {"type":"GENERAL","question":"Comment votre formation soutient-elle votre travail Java ?"}
                ]}
                """, "Java", "Développeur Java");

        List<InterviewAiClient.GeneratedQuestion> questions = testClient.client().generateQuestions(
                profile(), new InterviewAiClient.OfferContext(
                        "offer-1", "Développeur Java", "Backend Spring", List.of("Java", "Spring")));

        assertEquals(3, questions.size());
        assertEquals("TECHNIQUE", questions.getFirst().type());
        assertEquals("GENERALE", questions.getLast().type());
        testClient.server().verify();
    }

    @Test
    void fallsBackAfterPrimaryFailure() throws Exception {
        TestClient testClient = client("primary-model", "fallback-model");
        expectStatus(testClient.server(), "primary-model", HttpStatus.TOO_MANY_REQUESTS);
        expectContent(testClient.server(), "fallback-model", answerEvaluationJson());

        var evaluation = testClient.client().evaluate("TECHNIQUE", "Question ?", "Réponse.");

        assertEquals(82, evaluation.score());
        testClient.server().verify();
    }

    @Test
    void triesMultipleUniqueFallbackModelsInOrder() throws Exception {
        TestClient testClient = client("primary-model",
                " fallback-one , ,fallback-one, fallback-two ");
        expectStatus(testClient.server(), "primary-model", HttpStatus.NOT_FOUND);
        expectStatus(testClient.server(), "fallback-one", HttpStatus.SERVICE_UNAVAILABLE);
        expectContent(testClient.server(), "fallback-two", finalEvaluationJson());

        var evaluation = testClient.client().evaluateSession(List.of());

        assertEquals("Préparez des exemples précis.", evaluation.conseils());
        testClient.server().verify();
    }

    @Test
    void returnsClearUnavailableErrorWhenEveryModelFails() throws Exception {
        TestClient testClient = client("primary-model", "fallback-model");
        expectStatus(testClient.server(), "primary-model", HttpStatus.SERVICE_UNAVAILABLE);
        expectStatus(testClient.server(), "fallback-model", HttpStatus.NOT_FOUND);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> testClient.client().evaluate("TECHNIQUE", "Question ?", "Réponse."));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        assertTrue(error.getReason().contains("Tous les modèles Groq configurés"));
        testClient.server().verify();
    }

    @Test
    void doesNotRetryAnInvalidApiKey() throws Exception {
        TestClient testClient = client("primary-model", "fallback-model");
        expectStatus(testClient.server(), "primary-model", HttpStatus.UNAUTHORIZED);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> testClient.client().evaluate("TECHNIQUE", "Question ?", "Réponse."));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        assertTrue(error.getReason().contains("clé API"));
        testClient.server().verify();
    }

    @Test
    void fallsBackWhenPrimaryReturnsInvalidJson() throws Exception {
        TestClient testClient = client("primary-model", "fallback-model");
        expectContent(testClient.server(), "primary-model", "not valid json");
        expectContent(testClient.server(), "fallback-model", questionResponseJson());

        List<InterviewAiClient.GeneratedQuestion> questions =
                testClient.client().generateQuestions(profile(), null);

        assertEquals(3, questions.size());
        testClient.server().verify();
    }

    @Test
    void reportsEmptyResponseWhenNoModelReturnsContent() throws Exception {
        TestClient testClient = client("primary-model", "");
        expectBody(testClient.server(), "primary-model", "{\"choices\":[]}");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> testClient.client().evaluateSession(List.of()));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
        assertTrue(error.getReason().contains("aucune réponse JSON valide"));
        testClient.server().verify();
    }

    @Test
    void evaluatesAnIndividualAnswerAndPreservesStructuredFeedback() throws Exception {
        TestClient testClient = client("primary-model", "");
        expectContent(testClient.server(), "primary-model", answerEvaluationJson(),
                "Question réelle", "Réponse candidate");

        var evaluation = testClient.client().evaluate(
                "TECHNIQUE", "Question réelle ?", "Réponse candidate.");

        assertEquals(82, evaluation.score());
        assertEquals("Pertinente", evaluation.pertinence());
        assertEquals(List.of("Préciser un compromis"), evaluation.pointsManquants());
        testClient.server().verify();
    }

    @Test
    void producesFinalEvaluationFeedback() throws Exception {
        TestClient testClient = client("primary-model", "");
        expectContent(testClient.server(), "primary-model", finalEvaluationJson(),
                "Réponse réelle", "forces");

        var evaluation = testClient.client().evaluateSession(List.of(
                new InterviewAiClient.AnsweredQuestion("EXPERIENCE", "Question ?", "Réponse réelle",
                        new tn.iset.smartrecruit.cv_service.model.QuestionEntretien.Evaluation(
                                70, "Pertinente", "Correcte", List.of(), "Ajouter un exemple."))));

        assertEquals(List.of("Réponses structurées"), evaluation.forces());
        assertEquals("Préparez des exemples précis.", evaluation.conseils());
        testClient.server().verify();
    }

    @Test
    void reportsMissingApiKeyWhenAnInterviewOperationIsRequested() {
        TestClient testClient = client("", "", "");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> testClient.client().evaluate("TECHNIQUE", "Question ?", "Réponse."));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        assertTrue(error.getReason().contains("clé API Groq"));
        testClient.server().verify();
    }

    private static TestClient client(String model, String fallbacks) {
        return client(API_KEY, model, fallbacks);
    }

    private static TestClient client(String apiKey, String model, String fallbacks) {
        RestClient.Builder builder = RestClient.builder().baseUrl(API_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new TestClient(new InterviewAiClient(builder.build(), MAPPER, apiKey, model, fallbacks), server);
    }

    private static InterviewAiClient.ProfileContext profile() {
        return new InterviewAiClient.ProfileContext(
                "Développeur backend", "Profil Java",
                List.of(new Profil.Competence("Java", "Backend")),
                List.of(), List.of(), List.of(), List.of());
    }

    private static void expectContent(MockRestServiceServer server, String model, String content,
                                      String... promptTerms) throws Exception {
        String response = MAPPER.writeValueAsString(Map.of(
                "choices", List.of(Map.of("message", Map.of("content", content)))));
        var expectation = server.expect(requestTo(API_URL + "/chat/completions"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + API_KEY))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value(model))
                .andExpect(jsonPath("$.response_format.type").value("json_object"));
        for (String term : promptTerms) {
            expectation.andExpect(jsonPath("$.messages[0].content").value(
                    org.hamcrest.Matchers.containsString(term)));
        }
        expectation.andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    private static void expectBody(MockRestServiceServer server, String model, String responseBody) {
        server.expect(requestTo(API_URL + "/chat/completions"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + API_KEY))
                .andExpect(jsonPath("$.model").value(model))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    private static void expectStatus(MockRestServiceServer server, String model, HttpStatus status) {
        server.expect(requestTo(API_URL + "/chat/completions"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + API_KEY))
                .andExpect(jsonPath("$.model").value(model))
                .andRespond(withStatus(status).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"test failure\"}"));
    }

    private static String questionResponseJson() {
        return """
                {"questions":[
                  {"type":"TECHNIQUE","question":"Question technique Java ?"},
                  {"type":"EXPERIENCE","question":"Question expérience Java ?"},
                  {"type":"GENERALE","question":"Question générale Java ?"}
                ]}
                """;
    }

    private static String answerEvaluationJson() {
        return """
                {"score":82,"pertinence":"Pertinente","exactitude":"Correcte",
                 "pointsManquants":["Préciser un compromis"],"retour":"Ajoutez un exemple concret."}
                """;
    }

    private static String finalEvaluationJson() {
        return """
                {"forces":["Réponses structurées"],"faiblesses":["Manque de détails"],
                 "axesAmelioration":["Ajouter des exemples"],"conseils":"Préparez des exemples précis."}
                """;
    }

    private record TestClient(InterviewAiClient client, MockRestServiceServer server) {}
}
