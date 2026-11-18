import json, os, re, time
import logging
import traceback
import pymupdf  # PyMuPDF
from fastapi import FastAPI, UploadFile, HTTPException
from fastapi.responses import JSONResponse
from google import genai
from google.genai.errors import ClientError
from google.genai import types
from pydantic import BaseModel, Field, ValidationError, field_validator

MODEL = os.getenv("GEMINI_MODEL", "gemini-3.8-flash")
# Modèles de secours séparés par des virgules (utilisés si le principal est saturé)
FALLBACKS = [m.strip() for m in os.getenv("GEMINI_FALLBACK_MODELS", "").split(",") if m.strip()]

try:
    client = genai.Client()  # lit GEMINI_API_KEY
except Exception as error:
    logging.warning("Gemini client initialization unavailable (%s)", type(error).__name__)
    client = None  # sans clé, le service marche quand même en mode local

app = FastAPI(title="SmartRecruit OCR service")


class InterviewApiError(Exception):
    def __init__(self, status_code: int, error: str, message: str):
        self.status_code = status_code
        self.error = error
        self.message = message


INTERVIEW_ERROR_DETAILS = {
    "AI_QUOTA_EXCEEDED": (
        429, "The AI service has reached its usage limit. Please try again later."
    ),
    "AI_CONFIGURATION_ERROR": (
        503, "The AI service is not configured correctly. Please contact the administrator."
    ),
    "AI_CONNECTION_ERROR": (
        503, "Unable to connect to the AI service. Please check your internet connection and try again."
    ),
    "AI_TIMEOUT": (504, "The AI is taking too long to respond. Please try again."),
    "AI_TEMPORARILY_UNAVAILABLE": (
        503, "The AI service is temporarily unavailable. Please try again later."
    ),
    "AI_INVALID_RESPONSE": (
        502, "The AI couldn't generate a valid response. Please try again."
    ),
    "INSUFFICIENT_PROFILE": (
        400,
        "Your profile doesn't contain enough validated information to generate personalized interview questions. Please complete your profile first.",
    ),
    "AI_UNEXPECTED_ERROR": (
        500, "Something went wrong while preparing your AI response. Please try again later."
    ),
}


def _interview_api_error(error: str) -> InterviewApiError:
    status_code, message = INTERVIEW_ERROR_DETAILS[error]
    return InterviewApiError(status_code, error, message)


@app.exception_handler(InterviewApiError)
async def handle_interview_api_error(_, error: InterviewApiError):
    return JSONResponse(
        status_code=error.status_code,
        content={"success": False, "error": error.error, "message": error.message},
    )


class TranslateItem(BaseModel):
    id: str
    titre: str
    description: str


class TranslateRequest(BaseModel):
    target: str
    items: list[TranslateItem]


class TranslatedItem(BaseModel):
    id: str
    titre: str
    description: str
    sourceLanguage: str


class TranslateResponse(BaseModel):
    items: list[TranslatedItem]


class InterviewOffer(BaseModel):
    id: str
    titre: str
    description: str = ""
    competences: list[str] = Field(default_factory=list)


class InterviewProfile(BaseModel):
    competencesValidees: list[dict] = Field(default_factory=list)
    experiences: list[dict] = Field(default_factory=list)
    formations: list[dict] = Field(default_factory=list)
    titre: str = ""
    resume: str = ""
    langues: list[dict] = Field(default_factory=list)
    sections: list[dict] = Field(default_factory=list)


class InterviewQuestionsRequest(BaseModel):
    profil: InterviewProfile
    offre: InterviewOffer | None = None


class InterviewQuestion(BaseModel):
    type: str
    question: str

    @field_validator("type")
    @classmethod
    def normalize_type(cls, value: str) -> str:
        normalized = value.strip().upper()
        aliases = {"TECHNICAL": "TECHNIQUE", "GENERAL": "GENERALE"}
        return aliases.get(normalized, normalized)


class InterviewQuestionsResponse(BaseModel):
    questions: list[InterviewQuestion]


class InterviewAnswerRequest(BaseModel):
    type: str
    question: str
    reponse: str


class InterviewAnswerEvaluation(BaseModel):
    score: int
    pertinence: str
    exactitude: str
    pointsManquants: list[str]
    retour: str


class AnsweredInterviewQuestion(BaseModel):
    type: str
    question: str
    reponse: str
    evaluation: InterviewAnswerEvaluation


class InterviewFinalEvaluationRequest(BaseModel):
    reponses: list[AnsweredInterviewQuestion]


class InterviewFinalEvaluation(BaseModel):
    forces: list[str]
    faiblesses: list[str]
    axesAmelioration: list[str]
    conseils: str


class GeminiIndisponible(Exception):
    """Gemini est saturé, hors quota, ou le modèle est introuvable."""


PROMPT = """Tu analyses un CV (PDF, éventuellement scanné). Lis tout le document,
puis réponds UNIQUEMENT avec un JSON valide de cette forme, en laissant vide ce qui est absent :
{
  "texteBrut": "tout le texte du CV, tel que lu",
  "titre": "", "resume": "", "telephone": "", "localisation": "",
  "competences": [{"nom": "", "categorie": ""}],
  "experiences": [{"poste": "", "entreprise": "", "dateDebut": "", "dateFin": "", "description": ""}],
  "formations": [{"diplome": "", "etablissement": "", "dateDebut": "", "dateFin": ""}],
  "langues": [{"nom": "", "niveau": ""}]
}
N'invente rien. Dates au format AAAA-MM si possible. Les compétences sont courtes
(ex: "Java", "Spring Boot", "Docker")."""


# ---------------------------------------------------------------- lecture du PDF
def native_text(pdf: bytes) -> tuple[str, int]:
    doc = pymupdf.open(stream=pdf, filetype="pdf")
    return "\n".join(p.get_text() for p in doc).strip(), len(doc)


# ---------------------------------------------------------------- niveau 1 et 2 : Gemini
def call_gemini(pdf: bytes):
    """Réessaie si Gemini est occupé (503), puis passe aux modèles de secours."""
    if client is None:
        raise GeminiIndisponible("GEMINI_API_KEY absente")
    last = None
    for model in [MODEL] + FALLBACKS:
        for attempt in range(3):
            try:
                return client.models.generate_content(
                    model=model,
                    contents=[types.Part.from_bytes(data=pdf, mime_type="application/pdf"), PROMPT],
                    config=types.GenerateContentConfig(
                        response_mime_type="application/json", temperature=0),
                )
            except Exception as e:
                last = e
                msg = str(e)
                if "503" in msg or "UNAVAILABLE" in msg:
                    if attempt < 2:
                        time.sleep(3 * (attempt + 1))  # attend 3 s, puis 6 s
                    continue
                if "429" in msg or "RESOURCE_EXHAUSTED" in msg or "404" in msg or "NOT_FOUND" in msg:
                    break  # quota atteint ou modèle inconnu : modèle suivant
                raise  # autre erreur (clé invalide...) : on la montre telle quelle
    raise GeminiIndisponible(str(last))


# ---------------------------------------------------------------- niveau 3 : extraction locale
SKILLS = ["Java", "Spring Boot", "Spring", "Hibernate", "JPA", "Python", "JavaScript", "TypeScript",
          "Angular", "React", "Vue", "Node.js", "Express", "PHP", "Laravel", "Symfony", "C++", "C#",
          ".NET", "SQL", "MySQL", "PostgreSQL", "MongoDB", "Oracle", "Docker", "Kubernetes", "Git",
          "GitHub", "GitLab", "Jenkins", "Linux", "AWS", "Azure", "Maven", "REST", "HTML", "CSS",
          "Bootstrap", "Tailwind", "Flutter", "Android", "Kotlin", "Swift", "Figma", "UML", "Scrum",
          "Agile", "Jira", "Power BI", "Excel", "TensorFlow", "Machine Learning", "Django", "Flask",
          "FastAPI", "Firebase", "Selenium", "Postman", "MATLAB", "Arduino"]

LANGS = {"Français": r"fran[cç]ais|french", "Anglais": r"anglais|english", "Arabe": r"arabe|arabic",
         "Espagnol": r"espagnol|spanish", "Allemand": r"allemand|german", "Italien": r"italien|italian"}
LEVEL = r"\b([ABC][12]|natif|native|courant|fluent|bilingue|interm[ée]diaire|avanc[ée]|d[ée]butant|bon|moyen)\b"

CITIES = ["Tunis", "Bizerte", "Sfax", "Sousse", "Nabeul", "Ariana", "Ben Arous", "Monastir", "Gabès",
          "Kairouan", "Mahdia", "Manouba", "Béja", "Jendouba", "Tozeur", "Médenine", "Paris", "Lyon"]

DATE = re.compile(r"(?:\d{1,2}[/.\-])?(?:19|20)\d{2}")
H_EXP = re.compile(r"^(exp[ée]riences?( professionnelles?)?|parcours professionnel)\b", re.I)
H_FOR = re.compile(r"^(formations?|[ée]ducation|dipl[oô]mes?|[ée]tudes|cursus)\b", re.I)
H_END = re.compile(r"^(comp[ée]tences?|langues?|projets?|certifications?|loisirs?|centres? d.int[ée]r[êe]ts?|"
                   r"int[ée]r[êe]ts?|skills|languages|profil|contact|r[ée]f[ée]rences?)\b", re.I)
TITLE = re.compile(r"d[ée]veloppeu|ing[ée]nieur|engineer|developer|[ée]tudiant|stagiaire|analyste|"
                   r"designer|technicien|data|chef de projet", re.I)


def _blocks(lines: list[str]) -> list[list[str]]:
    res, cur = [], []
    for raw in lines:
        l = raw.strip()
        if not l:
            if cur:
                res.append(cur)
                cur = []
            continue
        if cur and DATE.search(l) and any(DATE.search(x) for x in cur):
            res.append(cur)
            cur = []
        cur.append(l)
    if cur:
        res.append(cur)
    return res[:8]


def extraction_locale(text: str) -> dict:
    """Extraction par règles : moins précise que l'IA, mais gratuite et toujours disponible."""
    lines = text.splitlines()

    phone = ""
    for m in re.finditer(r"(?:\+|00)?\d[\d\s.\-]{7,}\d", text):
        if 8 <= len(re.sub(r"\D", "", m.group(0))) <= 13:
            phone = m.group(0).strip()
            break

    titre = next((l.strip() for l in lines[:10] if TITLE.search(l) and len(l.strip()) <= 80), "")
    ville = next((c for c in CITIES if re.search(rf"\b{c}\b", text, re.I)), "")

    competences = [{"nom": s, "categorie": ""} for s in SKILLS
                   if re.search(rf"(?<![\w+#.]){re.escape(s)}(?![\w+#])", text, re.I)]

    langues = []
    for nom, pat in LANGS.items():
        line = next((l for l in lines if re.search(pat, l, re.I)), None)
        if line:
            lv = re.search(LEVEL, line, re.I)
            langues.append({"nom": nom, "niveau": lv.group(1) if lv else ""})

    sections: dict[str, list[str]] = {"exp": [], "for": []}
    current = None
    for l in lines:
        s = l.strip()
        if 0 < len(s) <= 40:
            if H_EXP.match(s):
                current = "exp"
                continue
            if H_FOR.match(s):
                current = "for"
                continue
            if H_END.match(s):
                current = None
                continue
        if current:
            sections[current].append(l)

    experiences = []
    for b in _blocks(sections["exp"]):
        d = [m.group(0) for m in DATE.finditer(" ".join(b))]
        experiences.append({
            "poste": DATE.sub("", b[0]).strip(" -–:"), "entreprise": "",
            "dateDebut": d[0] if d else "",
            "dateFin": d[1] if len(d) > 1 else ("Présent" if re.search(r"pr[ée]sent|actuel", " ".join(b), re.I) else ""),
            "description": " ".join(b[1:])[:400]})

    formations = []
    for b in _blocks(sections["for"]):
        d = [m.group(0) for m in DATE.finditer(" ".join(b))]
        formations.append({
            "diplome": DATE.sub("", b[0]).strip(" -–:"),
            "etablissement": DATE.sub("", b[1]).strip(" -–:") if len(b) > 1 else "",
            "dateDebut": d[0] if d else "", "dateFin": d[1] if len(d) > 1 else ""})

    return {"titre": titre, "resume": "", "telephone": phone, "localisation": ville,
            "competences": competences, "experiences": experiences,
            "formations": formations, "langues": langues}


# ---------------------------------------------------------------- endpoint
@app.post("/analyse")
def analyse(file: UploadFile):
    if file.content_type != "application/pdf":
        raise HTTPException(400, "PDF uniquement")
    pdf = file.file.read()
    try:
        natif, nb_pages = native_text(pdf)
    except Exception:
        raise HTTPException(422, "PDF illisible ou corrompu")

    data, secours, resp = None, False, None
    try:
        resp = call_gemini(pdf)
    except GeminiIndisponible:
        if len(natif) < 50:
            raise HTTPException(503, "Gemini est indisponible et ce PDF est scanné. Réessaie dans quelques minutes.")
        data, secours = extraction_locale(natif), True  # mode de secours
    except Exception as e:
        raise HTTPException(502, f"Service IA indisponible : {str(e)[:300]}")

    if data is None:
        raw = re.sub(r"^```(?:json)?|```$", "", (resp.text or "").strip()).strip()
        try:
            data = json.loads(raw)
        except json.JSONDecodeError:
            raise HTTPException(502, "Réponse IA invalide")
        if not isinstance(data, dict):
            raise HTTPException(502, "Réponse IA invalide")

    texte = natif if len(natif) >= 50 else data.get("texteBrut", "")
    data.pop("texteBrut", None)
    return {
        "texteOCR": texte,
        "ocrUtilise": len(natif) < 50,
        "modeSecours": secours,  # True = extraction locale (sans IA)
        "pages": nb_pages,
        "donnees": data,
    }


@app.post("/translate", response_model=TranslateResponse)
def translate(request: TranslateRequest):
    if len(request.items) > 20:
        raise HTTPException(400, "20 annonces maximum par requête")
    if not request.items:
        return TranslateResponse(items=[])
    if client is None:
        raise HTTPException(503, "GEMINI_API_KEY absente")

    prompt = """You translate job ads into English. For each item translate titre and description to English.
If the text is already English, return it unchanged. Keep technical terms, product names, programming languages
and frameworks exactly as written (Java, Spring Boot, Angular, MongoDB...). Do not add or remove information.
Return only a JSON array of objects with the keys id, titre, description, sourceLanguage (ISO 639-1 code).
The requested target language is ISO 639-1 code """ + request.target + """. This target takes precedence if it differs from English.
Translate this JSON array:
""" + json.dumps([
    {"id": item.id, "titre": item.titre, "description": item.description}
    for item in request.items
], ensure_ascii=False)

    try:
        response = client.models.generate_content(
            model=MODEL,
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json", temperature=0),
        )
    except Exception as e:
        import logging
        logging.exception("translate failed")
        message = str(e)
        api_key = os.getenv("GEMINI_API_KEY")
        if api_key:
            message = message.replace(api_key, "[REDACTED]")
        message = message.replace(prompt, "[REDACTED]")[:200]
        raise HTTPException(
            502,
            f"Service IA indisponible: {type(e).__name__}: {message}",
        )

    response_text = getattr(response, "text", None)
    raw = re.sub(
        r"^```(?:json)?|```$",
        "",
        response_text.strip() if isinstance(response_text, str) else "",
        flags=re.IGNORECASE,
    ).strip()
    try:
        translated = json.loads(raw)
        if not isinstance(translated, list):
            translated = []
    except (json.JSONDecodeError, TypeError):
        translated = []

    by_id = {}
    for item in translated:
        if not isinstance(item, dict):
            continue
        item_id = item.get("id")
        if not isinstance(item_id, str):
            continue
        if not all(isinstance(item.get(key), str)
                   for key in ("titre", "description", "sourceLanguage")):
            continue
        by_id[item_id] = item

    results = []
    for original in request.items:
        item = by_id.get(original.id)
        if item is None:
            results.append(TranslatedItem(
                id=original.id, titre=original.titre,
                description=original.description, sourceLanguage=""))
        else:
            results.append(TranslatedItem(
                id=original.id, titre=item["titre"],
                description=item["description"], sourceLanguage=item["sourceLanguage"]))

    return TranslateResponse(items=results)


def interview_gemini_json(prompt: str) -> dict:
    if client is None:
        raise _interview_api_error("AI_CONFIGURATION_ERROR")
    model_names = list(dict.fromkeys([MODEL, *FALLBACKS]))
    last_error = None

    for model in model_names:
        response = None
        for attempt in range(2):
            try:
                response = client.models.generate_content(
                    model=model,
                    contents=prompt,
                    config=types.GenerateContentConfig(
                        response_mime_type="application/json", temperature=0.2),
                )
                break
            except ClientError as error:
                last_error = error
                _log_interview_gemini_error(error, prompt, model, attempt + 1)
                error_code = _gemini_error_code(error)
                if error_code == 429:
                    raise _interview_api_error("AI_QUOTA_EXCEEDED") from error
                if error_code in {401, 403}:
                    raise _interview_api_error("AI_CONFIGURATION_ERROR") from error
                if error_code == 404:
                    break
                if error_code in {500, 502, 503, 504}:
                    if attempt == 0:
                        time.sleep(0.5)
                        continue
                    break
                if error_code is not None and 400 <= error_code < 500:
                    raise _interview_api_error("AI_CONFIGURATION_ERROR") from error
                raise _interview_api_error("AI_UNEXPECTED_ERROR") from error
            except Exception as error:
                last_error = error
                _log_interview_gemini_error(error, prompt, model, attempt + 1)
                error_code = _gemini_error_code(error)

                if error_code == 429:
                    raise _interview_api_error("AI_QUOTA_EXCEEDED") from error
                if error_code in {401, 403}:
                    raise _interview_api_error("AI_CONFIGURATION_ERROR") from error
                if error_code == 404:
                    break
                if error_code in {500, 502, 503, 504}:
                    if attempt == 0:
                        time.sleep(0.5)
                        continue
                    break
                if _is_interview_timeout_error(error):
                    raise _interview_api_error("AI_TIMEOUT") from error
                if _is_interview_connection_error(error):
                    if attempt == 0:
                        time.sleep(0.5)
                        continue
                    break

                raise _interview_api_error("AI_UNEXPECTED_ERROR") from error

        if response is not None:
            break

    if response is None:
        if last_error is None:
            raise _interview_api_error("AI_INVALID_RESPONSE")
        raise _interview_api_error_for_gemini(last_error) from last_error

    try:
        response_text = getattr(response, "text", None)
        if not isinstance(response_text, str) or not response_text.strip():
            logging.warning("Gemini returned an empty response.")
            raise _interview_api_error("AI_INVALID_RESPONSE")
        raw = re.sub(r"^```(?:json)?|```$", "", response_text.strip(),
                     flags=re.IGNORECASE).strip()
    except InterviewApiError:
        raise
    except Exception as error:
        _log_interview_gemini_error(error, prompt, MODEL, 1)
        raise _interview_api_error("AI_UNEXPECTED_ERROR") from error
    try:
        result = json.loads(raw)
    except (json.JSONDecodeError, TypeError) as error:
        logging.warning("Gemini returned invalid JSON (%s)", type(error).__name__)
        raise _interview_api_error("AI_INVALID_RESPONSE") from error
    if not isinstance(result, dict):
        raise _interview_api_error("AI_INVALID_RESPONSE")
    return result


def _interview_api_error_for_gemini(error: Exception) -> InterviewApiError:
    code = _gemini_error_code(error)
    if code == 429:
        return _interview_api_error("AI_QUOTA_EXCEEDED")
    if code in {401, 403, 404}:
        return _interview_api_error("AI_CONFIGURATION_ERROR")
    if _is_interview_timeout_error(error):
        return _interview_api_error("AI_TIMEOUT")
    if _is_interview_connection_error(error):
        return _interview_api_error("AI_CONNECTION_ERROR")
    if code in {500, 502, 503, 504}:
        return _interview_api_error("AI_TEMPORARILY_UNAVAILABLE")
    return _interview_api_error("AI_UNEXPECTED_ERROR")


def _gemini_error_code(error: Exception) -> int | None:
    current = error
    while current is not None:
        code = getattr(current, "code", None)
        if isinstance(code, int):
            return code
        response_status = getattr(getattr(current, "response", None), "status_code", None)
        if isinstance(response_status, int):
            return response_status
        current = current.__cause__ or current.__context__
    return None


def _is_interview_timeout_error(error: Exception) -> bool:
    current = error
    while current is not None:
        if isinstance(current, TimeoutError):
            return True
        for error_type in type(current).__mro__:
            module = getattr(error_type, "__module__", "").split(".", 1)[0]
            if module in {"httpx", "httpx2"} and error_type.__name__ in {
                "TimeoutException", "ConnectTimeout", "ReadTimeout", "WriteTimeout", "PoolTimeout",
            }:
                return True
        current = current.__cause__ or current.__context__
    return False


def _is_interview_connection_error(error: Exception) -> bool:
    current = error
    while current is not None:
        for error_type in type(current).__mro__:
            module = getattr(error_type, "__module__", "").split(".", 1)[0]
            if module in {"httpx", "httpx2"} and error_type.__name__ in {
                "ConnectError", "NetworkError",
            }:
                return True
        current = current.__cause__ or current.__context__
    return False


def _log_interview_gemini_error(error: Exception, prompt: str, model: str, attempt: int) -> None:
    message = str(error)
    traceback_text = "".join(traceback.format_tb(error.__traceback__))
    for sensitive_value in (os.getenv("GEMINI_API_KEY"), prompt):
        if sensitive_value:
            message = message.replace(sensitive_value, "[REDACTED]")
            traceback_text = traceback_text.replace(sensitive_value, "[REDACTED]")
    logging.error(
        "Interview Gemini request failed: model=%s attempt=%s exception_type=%s "
        "message=%s\nTraceback (most recent call last):\n%s",
        model, attempt, type(error).__name__, message, traceback_text,
    )


def _validate_interview_response(response_model, prompt: str):
    try:
        return response_model.model_validate(interview_gemini_json(prompt))
    except InterviewApiError:
        raise
    except ValidationError as error:
        logging.warning("Gemini response failed schema validation (%s)", type(error).__name__)
        raise _interview_api_error("AI_INVALID_RESPONSE") from error


@app.post("/interview/questions", response_model=InterviewQuestionsResponse)
def generate_interview_questions(request: InterviewQuestionsRequest):
    prompt = ""
    try:
        profile = request.profil.model_dump(exclude_none=True)
        if not any(
            isinstance(skill.get("nom"), str) and skill["nom"].strip()
            for skill in profile["competencesValidees"]
            if isinstance(skill, dict)
        ):
            raise _interview_api_error("INSUFFICIENT_PROFILE")
        offer = request.offre.model_dump(exclude_none=True) if request.offre else None
        prompt = """You are an experienced interviewer. Generate 8 varied interview questions dynamically from
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
""" + json.dumps(profile, ensure_ascii=False) + "\nJob offer JSON:\n" + json.dumps(offer, ensure_ascii=False)
        result = _validate_interview_response(InterviewQuestionsResponse, prompt)
        normalized_questions = [
            re.sub(r"\W+", " ", question.question.casefold(), flags=re.UNICODE).strip()
            for question in result.questions
        ]
        if not 3 <= len(result.questions) <= 15 or len(set(normalized_questions)) != len(normalized_questions) or any(
                q.type not in {"TECHNIQUE", "EXPERIENCE", "GENERALE"} or not q.question.strip()
                for q in result.questions):
            logging.warning("Gemini returned an invalid interview question set (count=%s)",
                            len(result.questions))
            raise _interview_api_error("AI_INVALID_RESPONSE")
        return result
    except InterviewApiError:
        raise
    except Exception as error:
        _log_interview_gemini_error(error, prompt, "question-generation", 1)
        raise _interview_api_error("AI_UNEXPECTED_ERROR") from error


@app.post("/interview/evaluate", response_model=InterviewAnswerEvaluation)
def evaluate_interview_answer(request: InterviewAnswerRequest):
    prompt = ""
    try:
        prompt = """Evaluate the candidate's answer against the exact interview question. Consider relevance,
technical correctness and depth appropriate to the question type. Do not reward unsupported claims. Identify
specific missing or incorrect points, and give constructive advice. Score from 0 to 100 based on this answer;
do not use a fixed or default score. Return only JSON:
{"score":0,"pertinence":"...","exactitude":"...","pointsManquants":["..."],"retour":"..."}.
Write feedback in French. Question type: """ + request.type + "\nQuestion: " + request.question + "\nCandidate answer: " + request.reponse
        result = _validate_interview_response(InterviewAnswerEvaluation, prompt)
        if not 0 <= result.score <= 100:
            raise _interview_api_error("AI_INVALID_RESPONSE")
        return result
    except InterviewApiError:
        raise
    except Exception as error:
        _log_interview_gemini_error(error, prompt, "answer-evaluation", 1)
        raise _interview_api_error("AI_UNEXPECTED_ERROR") from error


@app.post("/interview/final-evaluation", response_model=InterviewFinalEvaluation)
def evaluate_interview_session(request: InterviewFinalEvaluationRequest):
    prompt = ""
    try:
        prompt = """Review the candidate's actual interview questions, answers, and per-answer evaluations.
Summarize evidence-based strengths, weaknesses, actionable areas to improve, and general interview advice.
Do not introduce claims unsupported by the answers. Return only JSON:
{"forces":["..."],"faiblesses":["..."],"axesAmelioration":["..."],"conseils":"..."}.
Write feedback in French. Interview responses:\n""" + json.dumps(
            [answer.model_dump() for answer in request.reponses], ensure_ascii=False)
        return _validate_interview_response(InterviewFinalEvaluation, prompt)
    except InterviewApiError:
        raise
    except Exception as error:
        _log_interview_gemini_error(error, prompt, "final-evaluation", 1)
        raise _interview_api_error("AI_UNEXPECTED_ERROR") from error