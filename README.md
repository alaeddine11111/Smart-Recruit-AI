# Smart-Recruit-AI
An Intelligent recruitment and candidate support platform

SmartRecruit AI connects candidates and recruiters. Candidates upload their CV, the platform extracts their skills with OCR and AI, and matches them with job offers. Recruiters publish offers and manage applications.

> Semester integration project, ISET Bizerte.

## Architecture

smartrecruit/
├── backend/smartrecruit/
│   ├── auth-service/   users, login, JWT (port 8081)
│   ├── cv-service/     CV upload, storage, analysis result (port 8082)
│   └── offer-service/  companies, job offers, external offers, skills endpoints (port 8084)
├── ocr-service/        Python service: PDF to text to structured data (port 8083)
├── frontend/           Angular web app (port 4200)
└── README.md

Candidate flow: a candidate uploads a PDF through the Angular app. `cv-service` stores the PDF on disk, saves its metadata and analysis state in MongoDB, and sends the PDF to `ocr-service`. Extracted profile information is saved in MongoDB. Candidates can review detected skills, save only the ones they validate, and view their saved profile on the candidate dashboard.

## Getting started

### Prerequisites

- Java 21 and Maven (IntelliJ IDEA recommended for the Spring Boot code)
- Node.js 24 (or 22.22.3 and newer) and the Angular CLI: `npm install -g @angular/cli` (VS Code recommended for the front end)
- Python 3.10 or newer
- MongoDB Community Server, installed locally and running on `localhost:27017` (Docker is not needed)
- A free Gemini API key from Google AI Studio (for `ocr-service`)

### 1. MongoDB

Start a local MongoDB server on `localhost:27017`, or use the provided Compose file:

```powershell
docker compose -f backend\smartrecruit\docker-compose.yml up -d
```

### 2. Back end services

Run each service in its own terminal (or run its Spring Boot application from IntelliJ):

```powershell
cd backend\smartrecruit\auth-service
.\mvnw.cmd spring-boot:run
```

The auth API starts at `http://localhost:8081`. The first run creates the `smartrecruit_auth` database and a default admin account.

```powershell
cd backend\smartrecruit\cv-service
.\mvnw.cmd spring-boot:run
```

The CV API starts at `http://localhost:8082`. Installed Maven can also be used with `mvn spring-boot:run`.

### 3. Offers service

**Offers — port 8084:**

```powershell
cd backend\smartrecruit\offer-service
.\mvnw.cmd spring-boot:run
```

`offer-service` uses the same JWT secret as the other services. External offers are translated to English through `ocr-service`, so start the OCR service (step 4) before using the external offers page. See the "offer-service" API section below.

### 4. OCR service

In PowerShell:

```powershell
cd ocr-service
py -3.12 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
$env:GEMINI_API_KEY = "your-key-from-google-ai-studio"
$env:GEMINI_MODEL = "gemini-3.8-flash"
$env:GEMINI_FALLBACK_MODELS = "gemini-3.6-flash,gemini-3.7-flash,gemini-3.8-flash"
python -m uvicorn main:app --reload --port 8083
```


The key and model variables apply only to that terminal session. The OCR service defaults to `gemini-3.8-flash` if `GEMINI_MODEL` is not set. Its interactive API docs are at `http://localhost:8083/docs`.

To avoid showing the key on screen (screenshots, screen sharing), set it with a hidden prompt instead:

```powershell
$s = Read-Host "Gemini API key" -AsSecureString
$env:GEMINI_API_KEY = [System.Net.NetworkCredential]::new("", $s).Password
```

### 5. Front end

```bash
cd frontend
npm install
ng serve
```

Open `http://localhost:4200`. Keep the auth, CV, offers, OCR and frontend services running in their respective terminals.

### Configuration

Spring configuration is in each service's `src/main/resources/application.yml`. Local defaults are for development only; override secrets and connection strings through environment variables.

| Variable | Service | Purpose | Default (development) |
|----------|---------|---------|-----------------------|
| `MONGODB_URI` | auth-service | MongoDB connection | `mongodb://localhost:27017/smartrecruit_auth` |
| `MONGODB_URI` | cv-service | MongoDB connection | `mongodb://localhost:27017/smartrecruit_cv` |
| `JWT_SECRET` | auth-service and cv-service | Shared Base64 signing key; both services must use the same value | Development key in each `application.yml` |
| `ADMIN_EMAIL` | auth-service | First admin account | `admin@smartrecruit.tn` |
| `ADMIN_PASSWORD` | auth-service | First admin password | `Admin@12345` |
| `UPLOADS_DIR` | cv-service | Directory for uploaded PDF files | `./uploads` (relative to service working directory) |
| `OCR_URL` | cv-service | OCR and interview AI service base URL | `http://localhost:8083` |
| `GEMINI_API_KEY` | ocr-service | Personal Gemini API key | Required for Gemini analysis and interview preparation |
| `GEMINI_MODEL` | ocr-service | Gemini model name | `gemini-3.8-flash` |
| `GEMINI_FALLBACK_MODELS` | ocr-service | Comma-separated fallback model names | Empty |
| `MONGODB_URI` | offer-service | MongoDB connection | `mongodb://localhost:27017/smartrecruit_offers` |
| `OFFER_SERVICE_URL` | auth-service and cv-service | offer-service URL for application-scoped recruiter candidate lookups | `http://localhost:8084` |
| `JWT_SECRET` | offer-service | Must be the same value as in auth-service and cv-service | Development key in `application.yml` |
| `OCR_URL` | offer-service | OCR service base URL, used to translate external offers | `http://localhost:8083` |
| `JOOBLE_API_KEY` | offer-service | Optional Jooble key (the Jooble provider is disabled by default) | Empty |
| `ADZUNA_APP_ID`, `ADZUNA_APP_KEY` | offer-service | Optional Adzuna keys (the Adzuna provider is disabled by default) | Empty |

## Security and privacy

- **Never commit secrets.** Do not put a Gemini key, a database password or a real JWT secret in a file that goes to GitHub. If a key is ever pasted somewhere public, delete it in Google AI Studio and create a new one.
- Everyone uses their **own** Gemini key.
- The content of an uploaded CV is sent to Google's Gemini API for analysis. This is fine for the project, but it is personal data, so use test CVs when you can.
- Interview question generation uses validated profile skills, experience, education, and (when selected) the public offer description and required skills. Candidate answers are sent to Gemini for per-answer and final feedback; use test data when possible.
- `ocr-service` has no authentication yet. Do not expose it outside your own machine.
- The title and description of external job ads are sent to Google's Gemini API (through `ocr-service` `/translate`) to be translated to English. They are public job ads, not personal data.
- Uploaded CV PDFs are stored on disk; MongoDB contains their metadata, not the PDF bytes. Keep the upload directory out of Git (`uploads/` is ignored).

## Users and roles

| Role | How it is created | Access |
|------|-------------------|--------|
| `CANDIDAT` | Public registration | Own profile and CV |
| `RECRUTEUR` | Public registration (creates or joins a company) | Own offers and their applications |
| `ADMIN` | Seeded at startup, never through registration | `/api/admin/**` |

## API

### auth-service (port 8081)

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/auth/register/candidat` | Public | Create a candidate account |
| POST | `/api/auth/register/recruteur` | Public | Create a recruiter account and its company |
| POST | `/api/auth/login` | Public | Returns a JWT and the user |
| GET | `/api/auth/me` | Logged in | Current user |
| GET | `/api/admin/utilisateurs` | Admin | List all users |

Protected endpoints require a bearer token in the `Authorization` header.

### cv-service (port 8082)

All endpoints require a valid bearer token.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/cv` | Upload or replace the candidate's PDF (`multipart/form-data`, field `file`, PDF up to 5 MB) |
| GET | `/api/cv` | Get the current candidate's CV metadata |
| GET | `/api/cv/analyse` | Get the current candidate's latest OCR analysis status |
| GET | `/api/profil` | Get the current candidate's profile |
| PUT | `/api/profil` | Save profile edits and validated skills |
| POST | `/api/profil/valider` | Mark the profile as validated |

MongoDB stores CV metadata in `cvs`, profile data in `profils`, and OCR analysis status/results in `analyses` (database `smartrecruit_cv` by default). Uploaded PDF bytes are stored on disk. Profile skills detected by OCR are in `competencesDetectees`; candidate-approved skills are in `competencesValidees`. Legacy `competences` values are preserved separately because their origin cannot be determined reliably.

Interview preparation is stored in the same database in `sessions_preparation`. Only authenticated candidates can create, access, answer, and finish their own sessions. Answer submissions are evaluated by Gemini before being saved; finishing a session requires every answer to have a successful evaluation.

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/preparations` | Candidate | Start a general session with `{ "offreId": null }`, or a targeted session with a published offer ID |
| GET | `/api/preparations` | Candidate | List the candidate's sessions, newest first |
| GET | `/api/preparations/{id}` | Session owner | Resume or review a session |
| PUT | `/api/preparations/{id}/reponses` | Session owner | Evaluate and save one answer: `{ "questionId": "...", "reponse": "..." }` (maximum 5,000 characters) |
| POST | `/api/preparations/{id}/terminer` | Session owner | Finish after all answers are evaluated; returns overall mean score and AI-generated final feedback |

Interview questions and feedback use the existing `ocr-service` Gemini client and `GEMINI_MODEL`; there is no hardcoded question, evaluation, or score fallback. AI outages return an error and do not save an unevaluated answer or finish a session.

### cv-service matching (port 8082)

`POST /api/matching` accepts a JSON array of offers, each with `id`, `titre`,
`competencesRequises` (skill-name array), and optional `source`, `url`,
`entreprise`, `localisation` and `description`. Send the candidate JWT in the
`Authorization: Bearer <token>` header.
Only candidates can call this endpoint, and the candidate's MongoDB profile must
be validated. Candidate skills are read from that profile, never from the
request, and at least one validated skill is required. The response ranks offers by score and includes matched skills, missing
skills, and an explanation. Matching is exact after trimming, ignoring case and
accents; synonyms and Gemini-based AI are deliberately out of scope for this
first transparent version. The recommendations page loads published internal
offers from `offer-service` and searches external listings using the candidate's
validated skills and location. External scores use recognized technology terms
found in each listing's title and description, not employer-confirmed
requirements. Listings with no recognized technology terms are not scored.

External listings are translated to English by `offer-service` before they reach the recommendations page (see below), so the recognized technology terms found in a listing's title and description are matched in English.

### offer-service (port 8084)

All endpoints require a valid bearer token (same JWT as the other services). Roles: `RECRUTEUR` and `ADMIN` for `/api/recruteur/**`, any logged-in role for the others. Recruiters can only manage their own company and offers (`ADMIN` can manage any).

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET / PUT | `/api/recruteur/entreprise` | Recruiter, admin | Read / create-or-update my company |
| GET | `/api/entreprises/{id}` | Logged in | Company card shown on an offer |
| GET | `/api/recruteur/offres?statut=&titre=&page=&size=` | Recruiter, admin | My offers |
| GET | `/api/recruteur/offres/{id}` | Owner, admin | One of my offers |
| POST | `/api/recruteur/offres` | Recruiter, admin | Create an offer (a company is required first) |
| PUT | `/api/recruteur/offres/{id}` | Owner, admin | Update an offer |
| PATCH | `/api/recruteur/offres/{id}/statut?value=PUBLISHED` | Owner, admin | Publish / close (`DRAFT`, `PUBLISHED`, `CLOSED`) |
| DELETE | `/api/recruteur/offres/{id}` | Owner, admin | Delete an offer |
| GET | `/api/offres?titre=&localisation=&competences=java,spring&typeContrat=CDI&page=&size=` | Logged in | Browse published offers (filters combine, all listed skills must match) |
| GET | `/api/offres/{id}` | Logged in | Published offer detail |
| GET | `/api/offres-externes?q=&location=&page=` | Logged in | External offers, translated to English |

Offers are stored in the `offres_emploi` collection and companies in `entreprises` (database `smartrecruit_offers` by default). Skills are saved trimmed and in lowercase. Contract types: `CDI`, `CDD`, `STAGE`, `FREELANCE`, `ALTERNANCE`.

#### Skills endpoints (for AI skill matching)

These read-only endpoints return the skills used in **published** offers only (drafts and closed offers are never included). They are meant for the skill matching.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/offres/competences?q=&limit=` | Distinct skills as an array of strings (lowercase), most frequent first. Optional `q` is a case-insensitive "contains" filter; `limit` defaults to 100 (max 500) |
| GET | `/api/offres/competences/stats` | Skills with their number of offers: `[{ "competence": "java", "count": 12 }]` |
| GET | `/api/offres/index-competences` | All published offers in a light format, no pagination (max 1000): `id`, `titre`, `entrepriseNom`, `localisation`, `typeContrat`, `competences` |

Skills are plain lowercase strings with no alias mapping, so `springboot` and `spring boot` are different values. The matching service already trims, lowercases and removes accents on both sides. If `cv-service` calls these endpoints from the backend, it must forward the user's `Authorization` header.

#### External offers and translation

`GET /api/offres-externes` fetches listings from free job APIs, translates them to English and returns them with a link to the original posting. External offers are never stored in the database.

| Provider | Key needed | Coverage | Status |
|----------|-----------|----------|--------|
| Remotive | No | Remote tech jobs | Enabled in `application.yml` |
| Arbeitnow | No | Europe (mostly Germany) and remote jobs | Enabled in `application.yml` |
| Jooble | Yes (`JOOBLE_API_KEY`) | Country job sites | Disabled, optional |
| Adzuna | Yes (`ADZUNA_APP_ID`, `ADZUNA_APP_KEY`) | Per country, Tunisia not covered | Disabled, optional |

Settings in `offer-service/src/main/resources/application.yml`:

```yaml
external-jobs:
  arbeitnow:
    enabled: true
  remotive:
    enabled: true
  jooble:
    enabled: false
    api-key: ${JOOBLE_API_KEY:}
  translation:
    enabled: true
    url: ${OCR_URL:http://localhost:8083}
```

How translation works:
- `offer-service` sends the title and description of each listing to `POST /translate` on `ocr-service` (Gemini), in small batches.
- The response contains the English `titre` and `description`. The original text is kept in `titreOriginal` and `descriptionOriginal`, and `langueSource` holds the detected language (for example `de`). The page shows "Translated from DE" for translated offers.
- Each translation is cached in memory per offer, so an offer is translated only once. The first search can take 10 to 30 seconds; later searches are fast. The cache is cleared when `offer-service` restarts.
- If the OCR service is stopped, slow or rate limited, the original (untranslated) text is returned and a warning is logged. The page never breaks.
- Remotive and Arbeitnow downloads are cached in memory (6 hours and 1 hour) to respect their rate limits. Every listing keeps its source name and link back to the original posting.
- Set `external-jobs.translation.enabled: false` to disable translation.

### Candidate applications (offer-service)

Applications are stored separately in the `candidatures` collection of the
`smartrecruit_offers` database. A candidature contains the authenticated
candidate identity, offer ID, application date and one of three statuses:
`PENDING`, `ACCEPTED` or `REJECTED` (new applications start at `PENDING`).
The MongoDB compound index prevents the same candidate from applying twice to
the same offer.

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/candidatures/{offreId}` | Candidate | Apply to a published internal offer |
| GET | `/api/candidatures/mes` | Candidate | List the current candidate's applications with offer information |
| GET | `/api/candidatures/{id}` | Candidate owner | Get one application owned by the current candidate |

To test the feature, sign in as a candidate, open `/offres/{id}` and click
`Apply`. Then open `/mes-candidatures` to see the application history and status.
A second apply attempt should return `409 Conflict`; draft, closed or nonexistent
offers should return `404 Not Found`.

External-offer clicks are tracked separately in `candidatures_externes`; this
records the candidate, offer details, platform, original URL, click time and
`REDIRECTED` status. `POST /api/candidatures/externes` records a redirect and
`GET /api/candidatures/externes` lists redirects for the authenticated candidate.
This only confirms that SmartRecruit opened the external link, not that an
application was submitted or accepted by the external platform.

### Recruiter application management

Recruiter endpoints use the authenticated recruiter's JWT identity and only
return applications for offers they own. Admins retain the existing admin
override. Application review updates only the internal candidature status.

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/recruteur/candidatures/stats` | Recruiter, admin | Counts by status and offer |
| GET | `/api/recruteur/candidatures?offreId=&statut=&candidate=` | Recruiter, admin | Applications for owned offers |
| GET | `/api/recruteur/candidatures/{id}` | Owning recruiter, admin | Application and offer details |
| PATCH | `/api/recruteur/candidatures/{id}/statut` | Owning recruiter, admin | Update to `PENDING`, `ACCEPTED` or `REJECTED` |
| GET | `/api/recruteur/candidatures/{id}/candidate` | Owning recruiter, admin | Candidate identity used by the profile services for authorization |

The auth and CV services also expose application-scoped candidate lookups under
`/api/recruteur/candidatures/{id}`. They verify ownership with `offer-service`
before returning candidate contact, profile or CV metadata. CV contents remain
stored as before; no download endpoint is added.

### ocr-service (port 8083)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/analyse` | Multipart form, field `file` (PDF only). Returns `texteOCR`, `ocrUtilise`, `pages` and `donnees` (skills, experience, education, languages) |
| POST | `/translate` | JSON body `{ "target": "en", "items": [{ "id", "titre", "description" }] }`. Translates job ads to English with Gemini and returns `{ "items": [{ "id", "titre", "description", "sourceLanguage" }] }`. Used by `offer-service` for external offers (at most 20 items per request) |

Errors: `400` not a PDF, `422` unreadable PDF, `429` free Gemini limit reached, `502` Gemini unavailable or invalid answer. For `/translate`: `422` invalid request body, `502` with `Service IA indisponible: <error>` when the Gemini call fails.

## Front end pages

| Route | Description |
|-------|-------------|
| `/connexion` | Login |
| `/inscription` | Registration (candidate or recruiter) |
| `/recommandations` | Candidate recommendations ranked against published internal and external offers |
| `/mon-cv` | Candidate CV page: upload a PDF, review detected skills, save validated skills, and edit profile information |
| `/tableau-de-bord` | Candidate dashboard with saved profile, validated skills, experience and education; recruiter/admin workspace |
| `/offres` | Browse published internal offers |
| `/recruteur` | Recruiter offer and company management |
| `/recruteur/entreprise` | Recruiter company profile |
| `/recruteur/offres` | Recruiter offer list: publish, close, edit, delete |
| `/recruteur/offres/new` and `/recruteur/offres/:id/edit` | Post or edit an offer |
| `/offres/:id` | Published offer detail with the company card |
| `/offres/externes` | External offers (Remotive, Arbeitnow), translated to English, with an "Apply" link to the original site |
| `/recruteur/candidatures` | Recruiter application list with offer/status filters |
| `/recruteur/candidatures/:id` | Recruiter application details, candidate information and status actions |

## Status

- [x] Authentication with roles (candidate, recruiter, admin) and JWT
- [x] Angular login and register pages
- [x] OCR service: PDF analysis with Gemini
- [x] CV service: PDF upload, metadata and analysis storage, OCR integration
- [x] Candidate profile editing and persistence in MongoDB
- [x] Candidate review and persistence of validated skills
- [x] Candidate dashboard for saved profile information
- [x] Company and job-offer service with candidate browsing and recruiter management
- [x] Matching score API and recommendations page integrated with published internal offers and external listings
- [x] Job offers: publish (recruiter), browse (candidate)
- [x] External job offers (Remotive, Arbeitnow) with automatic translation to English
- [x] Skills endpoints on `offer-service` for AI skill matching
- [x] Applications and status tracking
- [ ] Interview preparation
- [ ] Recruiter and administrator dashboards

## Troubleshooting

| Problem | Cause and fix |
|---------|---------------|
| CV upload or profile requests return `ERR_CONNECTION_REFUSED` on port 8082 | Start `cv-service` and check its startup logs. |
| CV metadata does not appear in Compass | The CV service writes to `smartrecruit_cv` by default, not `smartrecruit_auth`. Refresh Compass after a successful upload and inspect the `cvs` collection. |
| Profile or analysis data is missing | Inspect `smartrecruit_cv` collections `profils` and `analyses`. A collection is created after its first document is saved. |
| CV upload is accepted, but analysis fails | Confirm OCR is running at `http://localhost:8083` and check the CV-service logs and latest analysis status. |
| Authenticated requests return `401` | Sign in again; confirm the frontend sends a bearer token and that `JWT_SECRET` matches in auth-service and cv-service. |
| OCR returns `502` with `404 NOT_FOUND` about the model | Set `GEMINI_MODEL` to a model available to your API key (for example `gemini-3.8-flash`). |
| OCR returns `429` | Gemini quota or rate limit reached. Wait and retry, or configure an available fallback model. |
| `$env:...` gives a syntax error | That syntax is for PowerShell. In Command Prompt use `set NAME=value`. |
| `ng` or `npm` errors at install | Check `node -v` against the prerequisites. |
| External offers are shown in German or another language | Translation is off or failing. Check `external-jobs.translation.enabled: true` in `offer-service`, that `ocr-service` is running on port 8083, and the `Translation` lines in the `offer-service` log. Restart `offer-service` after changing its code or settings. |
| `POST /translate` returns `502 Service IA indisponible: ...` | The text after the colon is the real Gemini error: wrong `GEMINI_MODEL` (404), invalid key (400/403) or quota (429). Set the key and model in the same terminal that runs `ocr-service`, then restart it. |
| `/offres/externes` shows "No external offers found" | Enable `remotive` or `arbeitnow` in `offer-service` `application.yml`, restart it, and try broader keywords with an empty location. |
| `.\mvnw.cmd` says `JAVA_HOME` is not defined correctly | Point `JAVA_HOME` to a JDK 21 folder in that terminal, for example `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot"`, and add `%JAVA_HOME%\bin` to `Path`. |
| `.\mvnw.cmd` is not recognized in a service folder | That service has no Maven wrapper: copy `mvnw`, `mvnw.cmd` and the `.mvn` folder from another service, or use an installed `mvn`. |

## Team

AlaEddine Abdelmalek, Mohamed Bejaoui, Ibrahim Mustapha Maazi, Refka Mimouni, Adem Zalaiti, Hassan Snoussi Jerbi.
