# Smart-Recruit-AI
An Intelligent recruitment and candidate support platform

SmartRecruit AI connects candidates and recruiters. Candidates upload their CV, the platform extracts their skills with OCR and AI, and matches them with job offers. Recruiters publish offers and manage applications.

> Semester integration project, ISET Bizerte.

## Tech stack

| Part | Technology |
|------|------------|
| Web front end | Angular (standalone components, signals, reactive forms) |
| Back end | Spring Boot 3 (Java 21), Spring Security, JWT |
| Database | MongoDB |
| Planned | Node.js services, OCR and AI matching, Android app |

## Project structure

```
smartrecruit/
├── backend/     auth-service: users, login, JWT (port 8081)
├── frontend/    Angular web app (port 4200)
└── README.md
```

A `cv-service` (CV upload and storage) will be added next to `backend` as a separate service.

## Getting started

### Prerequisites

- Java 21 and Maven (IntelliJ IDEA recommended for the back end)
- Node.js 24 (or 22.22.3 and newer) and the Angular CLI: `npm install -g @angular/cli`
- MongoDB Community Server running on `localhost:27017`
- VS Code recommended for the front end

### 1. Back end

1. Start MongoDB.
2. Open the `backend` folder in IntelliJ (open its `pom.xml` as a project).
3. Run `AuthServiceApplication`. The API starts on `http://localhost:8081`.

On first start, the app creates the database `smartrecruit_auth` and a default admin account.

### 2. Front end

```bash
cd frontend
npm install
ng serve
```

Open `http://localhost:4200`.

### Configuration

The defaults in `backend/src/main/resources/application.yml` are for local development only. Override them with environment variables outside your own machine:

| Variable | Purpose | Default (dev only) |
|----------|---------|--------------------|
| `MONGODB_URI` | MongoDB connection | `mongodb://localhost:27017/smartrecruit_auth` |
| `JWT_SECRET` | Base64 key that signs tokens | a dev key in `application.yml` |
| `ADMIN_EMAIL` | First admin account | `admin@smartrecruit.tn` |
| `ADMIN_PASSWORD` | First admin password | `Admin@12345` |

Never commit real secrets to GitHub.

## Users and roles

| Role | How it is created | Access |
|------|-------------------|--------|
| `CANDIDAT` | Public registration | Own profile and CV |
| `RECRUTEUR` | Public registration (creates or joins a company) | Offers and applications (coming) |
| `ADMIN` | Seeded at startup, never through registration | `/api/admin/**` |

## API (auth-service)

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/auth/register/candidat` | Public | Create a candidate account |
| POST | `/api/auth/register/recruteur` | Public | Create a recruiter account and its company |
| POST | `/api/auth/login` | Public | Returns a JWT and the user |
| GET | `/api/auth/me` | Logged in | Current user |
| GET | `/api/admin/utilisateurs` | Admin | List all users |

Send the token as `Authorization: Bearer <token>`.

## Front end pages

| Route | Description |
|-------|-------------|
| `/connexion` | Login |
| `/inscription` | Registration (candidate or recruiter) |
| `/mon-cv` | Candidate CV upload (PDF, 5 MB max) |
| `/tableau-de-bord` | Temporary test page for recruiters and admins |

## Status

- [x] Authentication with roles (candidate, recruiter, admin) and JWT
- [x] Angular login and register pages
- [x] Candidate CV upload page (interface only, waiting for the cv-service)
- [ ] cv-service: PDF upload, storage, one CV per candidate
- [ ] OCR extraction of CV data and skills validation
- [ ] Job offers: publish (recruiter), browse (candidate)
- [ ] Applications and status tracking
- [ ] AI matching between CVs and offers
- [ ] Interview preparation
- [ ] Role dashboards

## Team

Ala Eddine Abdelmalek
Ibrahim Mustapha Maazi
Mohamed Bejaoui
Refka Mimouni
Adem Zalaiti
Hassan Snoussi Jerbi.