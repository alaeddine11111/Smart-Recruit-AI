import json
import unittest
from unittest.mock import patch

from fastapi.testclient import TestClient
from google.genai.errors import ClientError

import main


class FakeResponse:
    def __init__(self, text):
        self.text = text


class FakeModels:
    def __init__(self, response=None, responses_by_model=None):
        self.response = response
        self.responses_by_model = responses_by_model or {}
        self.prompt = None
        self.models = []

    def generate_content(self, **kwargs):
        self.prompt = kwargs["contents"]
        model = kwargs["model"]
        self.models.append(model)
        response = self.responses_by_model.get(model, self.response)
        if isinstance(response, list):
            response = response.pop(0)
        if callable(response):
            response = response(kwargs["contents"])
        if isinstance(response, BaseException):
            raise response
        if hasattr(response, "text"):
            return response
        return type("Response", (), {"text": json.dumps(response)})()


class FakeGeminiApiError(Exception):
    def __init__(self, code, status, message):
        self.code = code
        self.status = status
        super().__init__(f"{code} {status}: {message}")


class ConnectError(Exception):
    pass


ConnectError.__module__ = "httpx2._exceptions"


class ReadTimeout(Exception):
    pass


ReadTimeout.__module__ = "httpx2._exceptions"


class InterviewAiTests(unittest.TestCase):
    def post_questions(self, models, fallback_models=()):
        client = type("Client", (), {"models": models})()
        with patch.object(main, "client", client), \
                patch.object(main, "MODEL", "primary"), \
                patch.object(main, "FALLBACKS", list(fallback_models)), \
                patch.object(main.time, "sleep") as sleep:
            response = TestClient(main.app).post("/interview/questions", json={
                "profil": {"competencesValidees": [{"nom": "Java"}]},
            })
        return response, sleep

    def assert_api_error(self, response, status, error, message):
        self.assertEqual(status, response.status_code)
        self.assertEqual({
            "success": False,
            "error": error,
            "message": message,
        }, response.json())

    def test_general_questions_use_validated_skills_experience_and_education(self):
        models = FakeModels({"questions": [
            {"type": "technique", "question": "Comment utilisez-vous Java ?"},
            {"type": "EXPERIENCE", "question": "Comment avez-vous utilisé Java dans votre expérience backend ?"},
            {"type": "GENERALE", "question": "Comment votre formation en informatique vous aide-t-elle en Java ?"},
        ]})
        with patch.object(main, "client", type("Client", (), {"models": models})()):
            result = main.generate_interview_questions(main.InterviewQuestionsRequest(
                profil=main.InterviewProfile(
                    titre="Développeuse backend",
                    resume="Développement de services web",
                    competencesValidees=[{"nom": "Java", "categorie": "Backend"}],
                    experiences=[{"poste": "Développeuse backend", "description": "Services REST"}],
                    formations=[{"diplome": "Informatique", "etablissement": "ISET"}],
                    langues=[{"nom": "Français", "niveau": "Courant"}],
                    sections=[{"titre": "Projets", "elements": ["Migration MongoDB"]}],
                ),
            ))
        self.assertEqual(3, len(result.questions))
        self.assertIn("Java", models.prompt)
        self.assertIn("Services REST", models.prompt)
        self.assertIn("Informatique", models.prompt)
        self.assertIn("Développeuse backend", models.prompt)
        self.assertIn("Français", models.prompt)
        self.assertIn("Migration MongoDB", models.prompt)
        self.assertNotIn("Pourquoi ce poste ?", models.prompt)
        self.assertNotIn('"question":"..."', models.prompt)
        self.assertEqual("TECHNIQUE", result.questions[0].type)

    def test_job_questions_use_candidate_profile_and_offer_requirements(self):
        models = FakeModels({"questions": [
            {"type": "TECHNIQUE", "question": "Comment Java peut-il répondre aux besoins backend de cette offre ?"},
            {"type": "EXPERIENCE", "question": "Comment vos services REST vous préparent-ils à ce poste Spring ?"},
            {"type": "GENERALE", "question": "Comment aborderiez-vous Spring si vous rejoigniez cette équipe ?"},
        ]})
        with patch.object(main, "client", type("Client", (), {"models": models})()):
            result = main.generate_interview_questions(main.InterviewQuestionsRequest(
                profil=main.InterviewProfile(
                    competencesValidees=[{"nom": "Java"}],
                    experiences=[{"poste": "Développeuse backend", "description": "Services REST"}],
                ),
                offre=main.InterviewOffer(
                    id="offer-1",
                    titre="Développeur Java",
                    description="Développement backend avec Spring",
                    competences=["Java", "Spring"],
                ),
            ))
        self.assertEqual(3, len(result.questions))
        self.assertIn("Java", models.prompt)
        self.assertIn("Développeur Java", models.prompt)
        self.assertIn("Services REST", models.prompt)
        self.assertIn("Spring", models.prompt)

    def test_rejects_combined_question_type_label(self):
        models = FakeModels({"questions": [
            {"type": "TECHNIQUE|EXPERIENCE|GENERALE", "question": "Décrivez votre approche."},
            {"type": "EXPERIENCE", "question": "Décrivez un projet."},
            {"type": "GENERALE", "question": "Pourquoi ce poste ?"},
        ]})
        response, _ = self.post_questions(models)
        self.assert_api_error(
            response, 502, "AI_INVALID_RESPONSE",
            "The AI couldn't generate a valid response. Please try again.",
        )

    def test_different_profiles_are_sent_to_gemini_and_produce_different_questions(self):
        def question_for_profile(prompt):
            profile_json = prompt.split("Candidate profile JSON:\n", 1)[1].split(
                "\nJob offer JSON:\n", 1
            )[0]
            profile = json.loads(profile_json)
            skill = profile["competencesValidees"][0]["nom"]
            return {"questions": [
                {"type": "TECHNIQUE", "question": f"Comment utilisez-vous {skill} ?"},
                {"type": "EXPERIENCE", "question": f"Décrivez un projet utilisant {skill}."},
                {"type": "GENERALE", "question": f"Comment votre expérience avec {skill} guide-t-elle votre travail ?"},
            ]}

        models = FakeModels(question_for_profile)
        with patch.object(main, "client", type("Client", (), {"models": models})()):
            java = main.generate_interview_questions(main.InterviewQuestionsRequest(
                profil=main.InterviewProfile(competencesValidees=[{"nom": "Java"}])
            ))
            angular = main.generate_interview_questions(main.InterviewQuestionsRequest(
                profil=main.InterviewProfile(competencesValidees=[{"nom": "Angular"}])
            ))

        self.assertEqual("Comment utilisez-vous Java ?", java.questions[0].question)
        self.assertEqual("Comment utilisez-vous Angular ?", angular.questions[0].question)

    def test_detected_skills_without_validated_skills_are_rejected(self):
        models = FakeModels({"questions": []})
        client = type("Client", (), {"models": models})()
        with patch.object(main, "client", client):
            response = TestClient(main.app).post("/interview/questions", json={
                "profil": {
                    "competencesValidees": [],
                    "competencesDetectees": [{"nom": "Python"}],
                },
            })

        self.assert_api_error(
            response,
            400,
            "INSUFFICIENT_PROFILE",
            "Your profile doesn't contain enough validated information to generate personalized interview questions. Please complete your profile first.",
        )
        self.assertEqual([], models.models)

    def test_duplicate_generated_questions_are_rejected(self):
        models = FakeModels({"questions": [
            {"type": "TECHNIQUE", "question": "Comment utilisez-vous Java ?"},
            {"type": "EXPERIENCE", "question": " Comment utilisez-vous Java! "},
            {"type": "GENERALE", "question": "Comment votre formation vous aide-t-elle ?"},
        ]})
        response, _ = self.post_questions(models)
        self.assert_api_error(
            response, 502, "AI_INVALID_RESPONSE",
            "The AI couldn't generate a valid response. Please try again.",
        )

    def test_quota_error_returns_friendly_structured_response_without_retry(self):
        valid_questions = {"questions": [
            {"type": "TECHNIQUE", "question": "Question technique ?"},
            {"type": "EXPERIENCE", "question": "Question expérience ?"},
            {"type": "GENERALE", "question": "Question générale ?"},
        ]}
        models = FakeModels(responses_by_model={
            "primary": [ClientError(429, {
                "error": {"status": "RESOURCE_EXHAUSTED", "message": "secret-api-key quota"},
            })],
            "fallback": [valid_questions],
        })
        with patch.dict("os.environ", {"GEMINI_API_KEY": "secret-api-key"}):
            with self.assertLogs(level="ERROR") as captured:
                response, sleep = self.post_questions(models, ["fallback"])

        self.assert_api_error(
            response, 429, "AI_QUOTA_EXCEEDED",
            "The AI service has reached its usage limit. Please try again later.",
        )
        self.assertEqual(["primary"], models.models)
        sleep.assert_not_called()
        logs = "\n".join(captured.output)
        self.assertIn("exception_type=ClientError", logs)
        self.assertIn("429 RESOURCE_EXHAUSTED", logs)
        self.assertNotIn("secret-api-key", logs)
        self.assertNotIn("secret-api-key", response.text)
        self.assertNotIn("quota", response.text)

    def test_quota_status_is_detected_from_client_error_response(self):
        response_with_status = type("HttpResponse", (), {"status_code": 429})()
        quota_error = ClientError(
            0,
            {"error": {"status": "RESOURCE_EXHAUSTED", "message": "quota"}},
            response=response_with_status,
        )
        self.assertIsNone(quota_error.code)
        response, sleep = self.post_questions(FakeModels(quota_error), ["fallback"])
        self.assert_api_error(
            response, 429, "AI_QUOTA_EXCEEDED",
            "The AI service has reached its usage limit. Please try again later.",
        )
        sleep.assert_not_called()

    def test_invalid_key_or_permission_returns_configuration_message_without_retry(self):
        for status_code, status in ((401, "UNAUTHENTICATED"), (403, "PERMISSION_DENIED")):
            with self.subTest(status_code=status_code):
                models = FakeModels(ClientError(status_code, {
                    "error": {"status": status, "message": "private detail"},
                }))
                response, sleep = self.post_questions(models, ["fallback"])
                self.assert_api_error(
                    response, 503, "AI_CONFIGURATION_ERROR",
                    "The AI service is not configured correctly. Please contact the administrator.",
                )
                self.assertEqual(["primary"], models.models)
                sleep.assert_not_called()
                self.assertNotIn("private detail", response.text)

    def test_connection_failure_returns_friendly_response_after_bounded_retry(self):
        models = FakeModels(responses_by_model={
            "primary": [ConnectError("DNS resolution failed"), ConnectError("DNS resolution failed")],
        })
        response, sleep = self.post_questions(models)
        self.assert_api_error(
            response, 503, "AI_CONNECTION_ERROR",
            "Unable to connect to the AI service. Please check your internet connection and try again.",
        )
        self.assertEqual(["primary", "primary"], models.models)
        sleep.assert_called_once_with(0.5)

    def test_timeout_returns_friendly_response_without_retry(self):
        models = FakeModels(ReadTimeout("upstream timed out"))
        response, sleep = self.post_questions(models)
        self.assert_api_error(
            response, 504, "AI_TIMEOUT",
            "The AI is taking too long to respond. Please try again.",
        )
        self.assertEqual(["primary"], models.models)
        sleep.assert_not_called()

    def test_gemini_server_error_returns_temporary_unavailable_after_bounded_retry(self):
        failure = FakeGeminiApiError(503, "UNAVAILABLE", "upstream detail")
        models = FakeModels(responses_by_model={"primary": [failure, failure]})
        response, sleep = self.post_questions(models)
        self.assert_api_error(
            response, 503, "AI_TEMPORARILY_UNAVAILABLE",
            "The AI service is temporarily unavailable. Please try again later.",
        )
        self.assertEqual(["primary", "primary"], models.models)
        sleep.assert_called_once_with(0.5)

    def test_invalid_or_empty_ai_response_returns_friendly_error(self):
        for model_response in (FakeResponse(""), FakeResponse("{invalid"), FakeResponse("null")):
            with self.subTest(response=model_response.text):
                models = FakeModels(model_response)
                response, _ = self.post_questions(models)
                self.assert_api_error(
                    response, 502, "AI_INVALID_RESPONSE",
                    "The AI couldn't generate a valid response. Please try again.",
                )

    def test_unexpected_exception_returns_generic_message_without_raw_details(self):
        models = FakeModels(RuntimeError("internal implementation detail"))
        response, _ = self.post_questions(models)
        self.assert_api_error(
            response, 500, "AI_UNEXPECTED_ERROR",
            "Something went wrong while preparing your AI response. Please try again later.",
        )
        self.assertNotIn("internal implementation detail", response.text)

    def test_model_not_found_uses_configured_fallback_model(self):
        valid_questions = {"questions": [
            {"type": "TECHNIQUE", "question": "Question technique ?"},
            {"type": "EXPERIENCE", "question": "Question expérience ?"},
            {"type": "GENERALE", "question": "Question générale ?"},
        ]}
        models = FakeModels(responses_by_model={
            "primary": [FakeGeminiApiError(404, "NOT_FOUND", "model not found")],
            "fallback": [valid_questions],
        })
        response, _ = self.post_questions(models, ["fallback"])
        self.assertEqual(200, response.status_code)
        self.assertEqual(["primary", "fallback"], models.models)
        self.assertEqual(3, len(response.json()["questions"]))

    def test_answer_evaluation_uses_structured_gemini_errors(self):
        models = FakeModels(FakeGeminiApiError(401, "UNAUTHENTICATED", "invalid key"))
        client = type("Client", (), {"models": models})()
        with patch.object(main, "client", client), \
                patch.object(main, "MODEL", "primary"), \
                patch.object(main, "FALLBACKS", []):
            response = TestClient(main.app).post("/interview/evaluate", json={
                "type": "TECHNIQUE",
                "question": "Question?",
                "reponse": "Answer.",
            })
        self.assert_api_error(
            response, 503, "AI_CONFIGURATION_ERROR",
            "The AI service is not configured correctly. Please contact the administrator.",
        )

    def test_answer_evaluation_uses_real_question_and_answer(self):
        models = FakeModels({
            "score": 82,
            "pertinence": "Réponse en lien avec la question",
            "exactitude": "Explication techniquement juste",
            "pointsManquants": ["Préciser le compromis"],
            "retour": "Ajoutez un exemple concret.",
        })
        with patch.object(main, "client", type("Client", (), {"models": models})()):
            result = main.evaluate_interview_answer(main.InterviewAnswerRequest(
                type="TECHNIQUE", question="Comment optimiser une requête SQL ?", reponse="J'ajoute un index."
            ))
        self.assertEqual(82, result.score)
        self.assertIn("Comment optimiser une requête SQL ?", models.prompt)
        self.assertIn("J'ajoute un index.", models.prompt)

    def test_final_feedback_uses_submitted_answers(self):
        models = FakeModels({
            "forces": ["Réponse structurée"],
            "faiblesses": ["Peu de détails"],
            "axesAmelioration": ["Ajouter des exemples"],
            "conseils": "Préparez des exemples précis.",
        })
        answer = main.AnsweredInterviewQuestion(
            type="EXPERIENCE",
            question="Décrivez un projet.",
            reponse="J'ai livré une API.",
            evaluation=main.InterviewAnswerEvaluation(
                score=70, pertinence="Pertinente", exactitude="Correcte",
                pointsManquants=["Résultat mesurable"], retour="Quantifiez l'impact.",
            ),
        )
        with patch.object(main, "client", type("Client", (), {"models": models})()):
            result = main.evaluate_interview_session(
                main.InterviewFinalEvaluationRequest(reponses=[answer])
            )
        self.assertIn("J'ai livré une API.", models.prompt)
        self.assertEqual(["Ajouter des exemples"], result.axesAmelioration)

    def test_unavailable_gemini_fails_explicitly(self):
        with patch.object(main, "client", None):
            response = TestClient(main.app).post("/interview/questions", json={
                "profil": {"competencesValidees": [{"nom": "Java"}]},
            })
        self.assert_api_error(
            response, 503, "AI_CONFIGURATION_ERROR",
            "The AI service is not configured correctly. Please contact the administrator.",
        )


if __name__ == "__main__":
    unittest.main()
