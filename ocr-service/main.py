import json, os, re
import fitz  # PyMuPDF, sert à savoir si le PDF contient du texte
from fastapi import FastAPI, UploadFile, HTTPException
from google import genai
from google.genai import types

MODEL = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
client = genai.Client()  # lit GEMINI_API_KEY dans l'environnement
app = FastAPI(title="SmartRecruit OCR service")

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


def native_text(pdf: bytes) -> tuple[str, int]:
    doc = fitz.open(stream=pdf, filetype="pdf")
    return "\n".join(p.get_text() for p in doc).strip(), len(doc)


@app.post("/analyse")
def analyse(file: UploadFile):
    if file.content_type != "application/pdf":
        raise HTTPException(400, "PDF uniquement")
    pdf = file.file.read()
    try:
        natif, nb_pages = native_text(pdf)
    except Exception:
        raise HTTPException(422, "PDF illisible ou corrompu")

    try:
        resp = client.models.generate_content(
            model=MODEL,
            contents=[types.Part.from_bytes(data=pdf, mime_type="application/pdf"), PROMPT],
            config=types.GenerateContentConfig(
                response_mime_type="application/json", temperature=0),
        )
    except Exception as e:
        msg = str(e)
        if "429" in msg or "RESOURCE_EXHAUSTED" in msg:
            raise HTTPException(429, "Limite gratuite Gemini atteinte, réessaie dans une minute.")
        raise HTTPException(502, f"Service IA indisponible : {msg[:300]}")

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
        "ocrUtilise": len(natif) < 50,  # PDF scanné : Gemini a fait l'OCR
        "pages": nb_pages,
        "donnees": data,
    }