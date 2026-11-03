# Smart-Recruit-AI
An Intelligent recruitment and candidate support platform

SmartRecruit AI connects candidates and recruiters. Candidates upload their CV, the platform extracts their skills with OCR and AI, and matches them with job offers. Recruiters publish offers and manage applications.

> Semester integration project, ISET Bizerte.

## Architecture

The project is split into independent services, one folder each:

| Folder | Role | Tech | Port | Status |
|--------|------|------|------|--------|
| `frontend/` | Web app | Angular | 4200 | In progress |
| `backend/` | auth-service: users, login, JWT | Spring Boot 3 (Java 21) | 8081 | Working |
| `ocr-service/` | Reads a CV PDF and extracts skills, experience, education | Python, FastAPI, Gemini | 8083 | Working |
| `cv-service/` | Stores the CV files, one per candidate | Spring Boot | 8082 | **Not built yet** |
| MongoDB | Database | MongoDB Community Server | 27017 | Local install |

Planned flow: the candidate uploads a PDF, `cv-service` stores it and calls `ocr-service`, the result is saved in MongoDB, and the candidate validates the detected skills.
For now, the CV page in the front end calls `ocr-service` directly, to show the detected skills. This is temporary.

## Getting started

### Prerequisites

- Java 21 and Maven (IntelliJ IDEA recommended for the Spring Boot code)
- Node.js 24 (or 22.22.3 and newer) and the Angular CLI: `npm install -g @angular/cli` (VS Code recommended for the front end)
- Python 3.10 or newer
- MongoDB Community Server, installed locally and running on `localhost:27017` (Docker is not needed)
- A free Gemini API key from Google AI Studio (for `ocr-service`)

### 1. Back end (auth-service)

1. Start MongoDB.
2. Open the `backend` folder in IntelliJ: open its `pom.xml` as a project, then wait for Maven to finish.
3. Run `AuthServiceApplication`. The API starts on `http://localhost:8081`.
4. cv-service (terminal 2) " cd backend\smartrecruit\cv-service
                            .\mvnw.cmd spring-boot:run"
5. ocr-service (terminal 3) : 
        cd ocr-service
        py -3.12 -m venv .venv
        .\.venv\Scripts\Activate.ps1
        python -m pip install -r requirements.txt
        $env:GEMINI_API_KEY = "generate every timeyou want run from this site web : https://aistudio.google.com/api-keys  ##api gemini 
        $env:GEMINI_MODEL = "gemini-3.8-flash"
        python -m uvicorn main:app --port 8083


On first start, the app creates the database `smartrecruit_auth` and a default admin account.

### 2. OCR service

```
cd ocr-service
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
```

Then set the variables and start it. The syntax depends on your terminal:

| Terminal | Set the variables |
|----------|-------------------|
| Command Prompt (`cmd`, prompt looks like `C:\...>`) | `set GEMINI_API_KEY=your_key` then `set GEMINI_MODEL=model_name` |
| PowerShell | `$env:GEMINI_API_KEY="your_key"` then `$env:GEMINI_MODEL="model_name"` (activate with `venv\Scripts\Activate.ps1`) |
| macOS / Linux | `export GEMINI_API_KEY=your_key` and `export GEMINI_MODEL=model_name`, activate with `source venv/bin/activate` |

```
uvicorn main:app --reload --port 8083
```

Replace `main` with the name of the Python file that contains `app`. The variables only exist in the terminal window where you set them.

Test it at `http://localhost:8083/docs`: open `POST /analyse`, upload a PDF, and check the JSON that comes back.

### 3. Front end

```bash
cd frontend
npm install
ng serve
```

Open `http://localhost:4200`.

### Configuration

Defaults in `backend/src/main/resources/application.yml` are for local development only. Override them with environment variables outside your own machine.

| Variable | Service | Purpose | Default (dev only) |
|----------|---------|---------|--------------------|
| `MONGODB_URI` | backend | MongoDB connection | `mongodb://localhost:27017/smartrecruit_auth` |
| `JWT_SECRET` | backend | Base64 key that signs tokens | a dev key in `application.yml` |
| `ADMIN_EMAIL` | backend | First admin account | `admin@smartrecruit.tn` |
| `ADMIN_PASSWORD` | backend | First admin password | `Admin@12345` |
| `GEMINI_API_KEY` | ocr-service | Your personal Gemini key | none, required |
| `GEMINI_MODEL` | ocr-service | Gemini model name | `gemini-2.5-flash` (see Troubleshooting) |

## Security and privacy

- **Never commit secrets.** Do not put a Gemini key, a database password or a real JWT secret in a file that goes to GitHub. If a key is ever pasted somewhere public, delete it in Google AI Studio and create a new one.
- Everyone uses their **own** Gemini key.
- The content of an uploaded CV is sent to Google's Gemini API for analysis. This is fine for the project, but it is personal data, so use test CVs when you can.
- `ocr-service` has no authentication yet. Do not expose it outside your own machine.
- CV files stored by the future `cv-service` must stay out of Git (`uploads/` is ignored).

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

Send the token as `Authorization: Bearer <token>`. You can test with Postman (the `.http` files only run in IntelliJ Ultimate).

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
| `/mon-cv` | Candidate CV page: choose a PDF (5 MB max), see the detected skills, keep or remove each one |
| `/tableau-de-bord` | Temporary test page for recruiters and admins |

## Status

- [x] Authentication with roles (candidate, recruiter, admin) and JWT
- [x] Angular login and register pages
- [x] OCR service: PDF analysis with Gemini
- [x] CV page: PDF selection and skills validation (in progress, validation is not saved yet)
- [ ] cv-service: PDF upload, storage, one CV per candidate, calls the OCR service
- [ ] Save the validated skills in MongoDB
- [ ] Job offers: publish (recruiter), browse (candidate)
- [ ] Applications and status tracking
- [ ] AI matching between CVs and offers
- [ ] Interview preparation
- [ ] Role dashboards

## Troubleshooting.

| Problem | Cause and fix |
|---------|---------------|
| IntelliJ says "out of the source root" | The project was copied without being imported. Close it, open the `pom.xml` as a project, then reload Maven. `pom.xml` must be directly inside `backend`. |
| Compass shows nothing after registering | Connect to `mongodb://localhost:27017`, refresh the database list, then open `smartrecruit_auth` and press Find in the collection. |
| `ERR_CONNECTION_REFUSED` on port 8082 | The `cv-service` does not exist yet. Nothing listens on 8082. |
| `$env:...` gives "syntax is incorrect" | You are in Command Prompt. Use `set NAME=value` there, or open PowerShell. |
| OCR returns `502` with `404 NOT_FOUND` about the model | The default model `gemini-2.5-flash` is refused for newer API keys. Set `GEMINI_MODEL` to a current model name from Google AI Studio (for example `gemini-3.8-flash`). |
| OCR returns `429` | Free Gemini limit reached. Wait a minute and retry. |
| Browser blocks the call to port 8083 (CORS) | `ocr-service` must allow `http://localhost:4200` with `CORSMiddleware` in `main.py`. |
| `ng` or `npm` errors at install | Check `node -v` against the prerequisites. |

## Team

AlaEddine Abdelmalek, Mohamed Bejaoui, Ibrahim Mustapha Maazi, Refka Mimouni, Adem Zalaiti, Hassan Snoussi Jerbi.
