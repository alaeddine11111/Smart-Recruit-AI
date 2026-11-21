# SmartRecruit mobile: context file

**How to use this file:** when you ask an AI assistant for help on the Android app, attach this file first,
then your request. Add the Java records (DTOs) of the backend endpoints you need, so field names are exact.
This file describes the real state of the code, not the plan.

---

## 1. Project

SmartRecruit AI connects candidates and recruiters (CV upload and analysis, job offers, matching, applications).
Semester project at ISET Bizerte. The **web part is mostly done** (Angular + Spring Boot microservices + MongoDB).
We now build the **Android app** in Kotlin. It is a course project: the teacher gave the course
"Introduction to Jetpack Compose" (composables, state, Column/Row/Box/Modifier, LazyColumn/LazyRow, Navigation, menus,
Kotlin basics, coroutines, Retrofit, Coil). The app must follow that course and use **MVVM**.

Repository layout (mobile lives next to the web part):

```
smartrecruit/
├── backend/smartrecruit/{auth-service, cv-service, offer-service}
├── ocr-service/
├── frontend/      Angular
└── mobile/        this Android app (open THIS folder in Android Studio)
```

Backend ports: auth 8081, cv 8082, ocr 8083, offers 8084. The main README is partly outdated: when README and code
disagree, the code (controllers and DTO records) is right.

## 2. Technical rules

- Kotlin, Jetpack Compose, Material 3, package `tn.smartrecruit`, min SDK 26, Gradle JVM 21.
- **MVVM**: Screen (Composable) -> ViewModel (exposes a `StateFlow` UiState) -> Repository -> Retrofit API / DataStore.
- **Manual dependency injection** with `AppContainer` + `ViewModelFactory` (no Hilt).
- Retrofit + Gson, OkHttp logging interceptor, DataStore for the session, Navigation Compose, Coil for images.
- UI text is French or English following the Figma design; code, class and property names are English.
- Repository methods return `Result<T>`; failures carry a user-readable message built by `Throwable.toUserMessage()` (`data/remote/ApiError.kt`).
- Composables keep form fields in local state (`rememberSaveable`); ViewModels hold the UiState and call repositories in `viewModelScope`.

## 3. Design (Figma)

Colors (in `ui/theme/Theme.kt`): GreenDark `#1B4D2E` (primary), GreenMid `#1F7A3E`, GreenLight `#E6F1E7`,
Orange `#B5651D` (accent), Cream `#FDF8F0` (background), TextDark `#1C2B20`, TextMuted `#6B7A70`.
Serif font for headings. Candidate app: top bar (notifications + overflow menu: profile, logout) and a bottom bar with 5 tabs:
**Home, Jobs, Applications, Interview, Profile**. Candidate home design: greeting, profile completion card,
"Let your skills shine" upload-CV card, recommended jobs (horizontal list with match %), recent application.

## 4. Current state of the code

Done (owner: Hassan, auth / roles / profile):
- Splash, welcome, choose role, login, candidate register, recruiter request form, "request pending" screen.
- Session in DataStore (token, role, name); the splash routes by role; logout.
- Profile screen for both roles: view, edit, change password.
- Candidate shell: `CandidateMainScreen` (top menu, bottom bar, inner NavHost). The Profile tab is real.
- Recruiter space: currently shows the profile screen only.

Placeholders (to be implemented by the teammates in charge of those features): candidate Home, Jobs, Applications,
Interview tabs; recruiter offers and applications screens. Each placeholder is a `composable(...)` in
`CandidateMainScreen.kt` (inner NavHost): replace it with your own screen.

Done: **Keycloak** login for the mobile app only (the teacher asked for it): the app logs in on Keycloak, then exchanges the Keycloak
token on `auth-service` (`POST /api/auth/keycloak`) for the normal SmartRecruit JWT. The web app and the other services are not
affected and never see Keycloak. See section 7b.

## 5. Package map

```
tn/smartrecruit/
├── MainActivity.kt, SmartRecruitApp.kt
├── data/model/        Role (enum: CANDIDAT, RECRUTEUR, ADMIN, UNKNOWN)
├── data/remote/       ApiClient (Retrofit clients + JWT interceptor), AuthApi, ApiError, dto/
├── data/local/        TokenStore (DataStore)
├── data/repository/   AuthRepository, ProfileRepository
├── di/                AppContainer, ViewModelFactory (+ appViewModelFactory() composable helper)
└── ui/
    ├── theme/  components/  navigation/ (Routes, AppNavGraph)
    └── screens/{auth, candidate, recruiter, profile}
```

## 6. How to add a feature (recipe)

1. **DTOs** in `data/remote/dto/`: copy the backend Java record; keep exact JSON names with `@SerializedName`
   (Kotlin property names in English). Nullable + default value for fields that may be missing.
2. **API interface** (`XxxApi.kt`) with `suspend` functions. Add a `val xxxApi by lazy { retrofit(ApiConfig.XXX_URL).create(...) }`
   in `ApiClient` (URLs for cv `8082` and offers `8084` already exist in `ApiConfig`).
3. **Repository** in `data/repository/` returning `Result<T>` (copy the `try/catch` + `toUserMessage()` pattern of `ProfileRepository`).
4. Register it in **`AppContainer`** (`val xxxRepository by lazy { ... }`).
5. **ViewModel** with a UiState (data class or sealed interface) exposed as `StateFlow`; add it to **`ViewModelFactory`**.
6. **Screen** in `ui/screens/<feature>/`, get the ViewModel with `viewModel(factory = appViewModelFactory())`,
   collect with `collectAsStateWithLifecycle()`. Use lists (`LazyColumn`/`LazyRow`) with a `key`.
7. Add the route (`Routes.kt` + `AppNavGraph.kt`, or a tab in `CandidateMainScreen.kt`).

Shared UI pieces: `AppTextField`, `PrimaryButton`, `ErrorText`, `AuthScaffold` in `ui/components/Components.kt`.
Reuse them, and add new shared composables there.

## 7. Backend facts a mobile developer needs

Authentication (auth-service):
- JWT in the header `Authorization: Bearer <token>`. The interceptor in `ApiClient` adds it everywhere except login/register.
  Token lifetime is 24 h. Roles: `CANDIDAT`, `RECRUTEUR`, `ADMIN`.
- Login: `POST /api/auth/login` body `{ "email", "motDePasse" }` ->
  `{ "token", "tokenType", "expiresInMs", "user": { "id", "nom", "email", "role", "dateCreation" } }`.
- Candidate registration: `POST /api/auth/register/candidat` body `{ nom, email, motDePasse (min 8), telephone, localisation }`.
- Recruiter registration: `POST /api/auth/register/recruteur` body
  `{ nom, email, motDePasse, poste, entrepriseNom, entrepriseDescription, entrepriseSiteWeb }`.
  It creates a **request**, not an account. An admin accepts or refuses it on the web. Until then, login answers `403`
  with a message ("en cours d'examen" or "refusée" + reason). Mobile shows that message.
- Profile: `GET/PUT /api/auth/profil`, password: `PUT /api/auth/mot-de-passe` `{ ancienMotDePasse, nouveauMotDePasse }`.
- Errors have the shape `{ "status": 400, "message": "...", "errors": { "field": "reason" } }` (`errors` only on validation failures).
- Admins use the web app only. The mobile app refuses an `ADMIN` login.
- Default dev admin (web): `admin@smartrecruit.tn` / `Admin@12345`.

Other services: see the main README (offer-service 8084: offers, companies, applications; cv-service 8082: CV upload, profile
skills, matching, interview preparation; ocr-service 8083: PDF analysis). They accept the same JWT.

## 7b. Keycloak (mobile only)

- Keycloak runs locally in dev mode on **port 8180** (realm `smartrecruit`, realm roles `CANDIDAT`/`RECRUTEUR`/`ADMIN`,
  public client `smartrecruit-mobile` with Direct access grants). The realm is created by importing `mobile/keycloak/smartrecruit-realm.json`
  (no manual setup, no users in the file). Team setup steps: `README.md`, section *Read this first*.
- Login flow in `AuthRepository.login`: `KeycloakApi.token(...)` (password grant, `scope=openid`) ->
  `AuthApi.loginWithKeycloak(KeycloakLoginRequest(accessToken))` -> SmartRecruit JWT + user, saved in `TokenStore`.
  Everything after login (interceptor, other services) is unchanged: it uses the SmartRecruit JWT only.
- `KeycloakConfig.USE_KEYCLOAK` (data/remote) switches back to the old login through `auth-service` if Keycloak is not running.
  Developers who do not work on the login can set it to `false` **locally** and must not commit that change.
- The Keycloak address is `ApiConfig.KEYCLOAK_URL` (same host as the other services, port 8180).
- Keycloak error messages ("Invalid user credentials", "Account disabled" for a pending recruiter, ...) are translated in `ApiError.kt`.
- `auth-service` has Keycloak **disabled by default**; it is enabled with `KEYCLOAK_ENABLED=true` (plus `KEYCLOAK_ADMIN_USER` and
  `KEYCLOAK_ADMIN_PASSWORD` in the terminal, never in a file). When enabled it creates/enables/deletes the matching Keycloak
  users on registration, recruiter acceptance and refusal. A mobile login needs the user to exist in Keycloak: an old account is copied
  the first time its owner logs in through `POST /api/auth/login`.
- The role used by the app comes from MongoDB (user with the same email), not from the Keycloak token.
- No Keycloak token refresh: when the SmartRecruit JWT expires (24 h), the user logs in again.

## 8. Running against the backend

- Emulator: backend address `http://10.0.2.2` (default).
- Real phone: set `HOST` in `data/remote/ApiClient.kt` to the PC's LAN IP, or use USB with `adb reverse`. Details in `README.md`.
- Keycloak must be running (port 8180, also reachable from the phone) and `auth-service` started with `KEYCLOAK_ENABLED=true`.
- The app uses `http` in dev, so the manifest has `usesCleartextTraffic="true"` and the `INTERNET` permission.
- **Never commit your personal IP** in `ApiClient.kt`.

## 9. Do not break (shared code)

`ApiClient`, `TokenStore`, `AuthRepository`, `AppContainer`, `ViewModelFactory`, `AppNavGraph` and `Components.kt` are shared:
adding to them is fine, but renaming or changing existing behavior must be agreed with the team (Hassan owns auth/session).
Do not add a second way to store the token or to log in: use `TokenStore` and `AuthRepository`. Other features must not talk to Keycloak:
they use the SmartRecruit JWT like before.

## 10. Tips for AI-assisted work

- Attach this file, then the exact backend DTO records and the controller for your endpoint.
- Ask for complete files with their full path under `tn/smartrecruit/`, and tell the assistant which existing files it may change.
- Ask for the register steps (AppContainer, ViewModelFactory, navigation) every time, they are the part that is easy to forget.
- When something fails, send the Logcat lines (filter `OkHttp` for network, `FATAL EXCEPTION` for crashes).
