import json, os, re, time
import pymupdf  # PyMuPDF
from fastapi import FastAPI, UploadFile, HTTPException
from google import genai
from google.genai import types

MODEL = os.getenv("GEMINI_MODEL", "gemini-3.8-flash")
# Modèles de secours séparés par des virgules (utilisés si le principal est saturé)
FALLBACKS = [m.strip() for m in os.getenv("GEMINI_FALLBACK_MODELS", "").split(",") if m.strip()]

try:
    client = genai.Client()  # lit GEMINI_API_KEY
except Exception:
    client = None  # sans clé, le service marche quand même en mode local

app = FastAPI(title="SmartRecruit OCR service")


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