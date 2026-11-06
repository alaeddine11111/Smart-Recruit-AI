# Smart-Recruit-AI
An Intelligent recruitment and candidate support platform

SmartRecruit AI connects candidates and recruiters. Candidates upload their CV, the platform extracts their skills with OCR and AI, and matches them with job offers. Recruiters publish offers and manage applications.

> Semester integration project, ISET Bizerte.

## Architecture

smartrecruit/
├── backend/smartrecruit/
│   ├── auth-service/   users, login, JWT (port 8081)
│   └── cv-service/     CV upload, storage, analysis result (port 8082)
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
mvn spring-boot:run
```

The auth API starts at `http://localhost:8081`. The first run creates the `smartrecruit_auth` database and a default admin account.

```powershell
cd backend\smartrecruit\cv-service
.\mvnw.cmd spring-boot:run
```

The CV API starts at `http://localhost:8082`. Installed Maven can also be used with `mvn spring-boot:run`.

### 3. OCR service

In PowerShell:

```powershell
cd ocr-service
py -3.12 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
$env:GEMINI_API_KEY = "your-key-from-google-ai-studio"
$env:GEMINI_MODEL = "gemini-3.8-flash"
python -m uvicorn main:app --reload --port 8083
```

The key and model variables apply only to that terminal session. The OCR service defaults to `gemini-3.8-flash` if `GEMINI_MODEL` is not set. Its interactive API docs are at `http://localhost:8083/docs`.

### 4. Front end

```bash
cd frontend
npm install
ng serve
```

Open `http://localhost:4200`. Keep the auth, CV, OCR and frontend services running in their respective terminals.

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
| `OCR_URL` | cv-service | OCR service base URL | `http://localhost:8083` |
| `GEMINI_API_KEY` | ocr-service | Personal Gemini API key | Required for Gemini analysis |
| `GEMINI_MODEL` | ocr-service | Gemini model name | `gemini-3.8-flash` |
| `GEMINI_FALLBACK_MODELS` | ocr-service | Comma-separated fallback model names | Empty |

## Security and privacy

- **Never commit secrets.** Do not put a Gemini key, a database password or a real JWT secret in a file that goes to GitHub. If a key is ever pasted somewhere public, delete it in Google AI Studio and create a new one.
- Everyone uses their **own** Gemini key.
- The content of an uploaded CV is sent to Google's Gemini API for analysis. This is fine for the project, but it is personal data, so use test CVs when you can.
- `ocr-service` has no authentication yet. Do not expose it outside your own machine.
- Uploaded CV PDFs are stored on disk; MongoDB contains their metadata, not the PDF bytes. Keep the upload directory out of Git (`uploads/` is ignored).

## Users and roles

| Role | How it is created | Access |
|------|-------------------|--------|
| `CANDIDAT` | Public registration | Own profile and CV |
| `RECRUTEUR` | Public registration (creates or joins a company) | Offers and applications (coming) |
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

### ocr-service (port 8083)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/analyse` | Multipart form, field `file` (PDF only). Returns `texteOCR`, `ocrUtilise`, `pages` and `donnees` (skills, experience, education, languages) |

Errors: `400` not a PDF, `422` unreadable PDF, `429` free Gemini limit reached, `502` Gemini unavailable or invalid answer.

## Front end pages

| Route | Description |
|-------|-------------|
| `/connexion` | Login |
| `/inscription` | Registration (candidate or recruiter) |
| `/mon-cv` | Candidate CV page: upload a PDF, review detected skills, save validated skills, and edit profile information |
| `/tableau-de-bord` | Candidate dashboard with saved profile, validated skills, experience and education; recruiter/admin workspace |

## Status

- [x] Authentication with roles (candidate, recruiter, admin) and JWT
- [x] Angular login and register pages
- [x] OCR service: PDF analysis with Gemini
- [x] CV service: PDF upload, metadata and analysis storage, OCR integration
- [x] Candidate profile editing and persistence in MongoDB
- [x] Candidate review and persistence of validated skills
- [x] Candidate dashboard for saved profile information
- [ ] Job offers: publish (recruiter), browse (candidate)
- [ ] Applications and status tracking
- [ ] AI matching between CVs and offers
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

## Team

AlaEddine Abdelmalek, Mohamed Bejaoui, Ibrahim Mustapha Maazi, Refka Mimouni, Adem Zalaiti, Hassan Snoussi Jerbi.
