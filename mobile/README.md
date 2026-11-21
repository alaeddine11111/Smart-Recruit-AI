# SmartRecruit AI: Android app

Native Android app (Kotlin, Jetpack Compose, MVVM) for the SmartRecruit AI platform.
It talks to the same Spring Boot microservices as the Angular web app.

> Semester integration project, ISET Bizerte. Context file for teammates and AI assistants: [`MOBILE_CONTEXT.md`](MOBILE_CONTEXT.md).

## Read this first: do I need Keycloak?

Keycloak is only used for the **login of the mobile app** (the teacher asked for it). It is **off by default**, so most of the team
does not have to do anything. Find your case:

| What you work on | What you have to do |
|------------------|---------------------|
| Web app, offer-service, cv-service, ocr-service, `auth-service` without mobile login | **Nothing.** Keycloak is disabled unless you set `KEYCLOAK_ENABLED=true`. |
| Mobile screens other than the login (jobs, applications, interview, recruiter screens) | **Nothing to install.** On your machine only, set `USE_KEYCLOAK = false` in `data/remote/KeycloakConfig.kt` (do **not** commit that change). The app then logs in through `auth-service` directly, with any account that exists in the web app. |
| The real Keycloak login on the phone (auth work, demo, final test) | Do the **6 steps below**, once (about 10 minutes). |

### The 6 steps (full Keycloak login)

Each person runs their own Keycloak on their own PC. Nothing is shared and nothing has to be created by hand in it:
the realm, the three roles and the mobile client come from the file [`keycloak/smartrecruit-realm.json`](keycloak/smartrecruit-realm.json).
Users are not in the file on purpose: `auth-service` creates them in Keycloak by itself (registration) or when an existing account logs in.

1. **Install Keycloak.** Download the zip from keycloak.org and extract it, for example to `C:\keycloak\keycloak-26.x`.
   You need a full **JDK 21** (the same as for the Spring Boot services).
2. **Import the realm.** Create the folder `C:\keycloak\keycloak-26.x\data\import` and copy `keycloak/smartrecruit-realm.json` into it.
3. **Start Keycloak** in `cmd` (the first time it also creates the admin account `admin` / `admin`):
   ```
   set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot
   set KC_BOOTSTRAP_ADMIN_USERNAME=admin
   set KC_BOOTSTRAP_ADMIN_PASSWORD=admin
   cd C:\keycloak\keycloak-26.x
   bin\kc.bat start-dev --http-port=8180 --http-host=0.0.0.0 --import-realm
   ```
   Check `http://localhost:8180`: the realm list must contain `smartrecruit`.
4. **Start `auth-service` with Keycloak on** (PowerShell, in the same terminal as the start command):
   ```powershell
   $env:KEYCLOAK_ENABLED = "true"
   $env:KEYCLOAK_ADMIN_USER = "admin"
   $env:KEYCLOAK_ADMIN_PASSWORD = "admin"
   .\mvnw.cmd spring-boot:run
   ```
5. **Real phone only:** allow port 8180 in the Windows firewall (administrator terminal):
   `netsh advfirewall firewall add rule name="SmartRecruit Keycloak" dir=in action=allow protocol=TCP localport=8180`
   and set your own `HOST` in `ApiClient.kt` (see *Backend address*). The emulator needs nothing.
6. **Accounts that already existed** (created before Keycloak): log in once with `POST http://localhost:8081/api/auth/login`
   (Postman) or on the web app. `auth-service` then copies the account to Keycloak. Accounts created from now on (web or mobile) are created in Keycloak automatically.

If the import file does not work on your Keycloak version, create the realm by hand instead (section *Keycloak (mobile login)*, step 2).

### What happens behind the scenes (so nothing is a surprise)

| Event | In Keycloak |
|-------|-------------|
| A candidate registers (web or mobile) | user created, enabled, role `CANDIDAT` |
| A recruiter sends a request | user created **disabled**, no role: login on the phone says the account is not activated |
| The admin accepts the request (web) | user enabled, role `RECRUTEUR` |
| The admin refuses the request (web) | user deleted, so the same email can ask again |
| An old account logs in through `/api/auth/login` | copied to Keycloak if missing |

If Keycloak is stopped, registration and the web app still work (a warning is logged), but that account cannot log in on mobile
until it exists in Keycloak.

## Features

| Area | Status |
|------|--------|
| Splash, welcome, choose role | Done |
| Candidate registration and login (JWT) | Done |
| Recruiter account request (needs admin approval) and "pending / refused" messages on login | Done |
| Session kept on the phone, logout | Done |
| Role-based navigation (candidate space / recruiter space) | Done |
| User profile: view, edit, change password (candidate and recruiter) | Done |
| Login through Keycloak (mobile only, requested by the teacher) | Done |
| Accounts created from the app are also created in Keycloak (candidate: active with role; recruiter: disabled until the admin accepts) | Done |
| Candidate Home, Jobs, Applications, Interview tabs | Placeholders |
| Recruiter offers and applications screens | Placeholders |

Administrators use the web app only: an admin account that logs in on mobile is refused.

## Requirements

- Android Studio (recent version) with an Android SDK
- **JDK 21** for Gradle (see Troubleshooting if the build asks for another one)
- Min SDK 26
- The backend running locally: MongoDB and `auth-service` (port 8081) with Keycloak enabled. Other services only when a screen needs them.
- **Keycloak** running locally on port **8180** (see *Keycloak* below).

## Getting started

1. Open the **`mobile`** folder in Android Studio (not the repository root) and let Gradle sync.
2. In **Settings > Build, Execution, Deployment > Build Tools > Gradle**, set the Gradle JVM version to **21**.
3. Start MongoDB, Keycloak and `auth-service` with Keycloak enabled (see *Keycloak* below).
4. Set the backend address (next section), then run the app.

### Backend address

The address is the `HOST` constant in
`app/src/main/java/tn/smartrecruit/data/remote/ApiClient.kt`:

```kotlin
private const val HOST = "http://10.0.2.2"
```

| Where the app runs | Value of `HOST` |
|--------------------|-----------------|
| Android emulator | `http://10.0.2.2` (default) |
| Real phone, same Wi-Fi as the PC | `http://<LAN IP of the PC>` (run `ipconfig`, use the IPv4 address) |
| Real phone, USB cable | `http://localhost` + run `adb reverse tcp:8081 tcp:8081` (same for 8082, 8084) |

Rules: start with `http://`, no port, no trailing slash. For the Wi-Fi option, allow the ports in the Windows firewall:

```
netsh advfirewall firewall add rule name="SmartRecruit dev" dir=in action=allow protocol=TCP localport=8081,8082,8084
```

Keycloak uses the same host on port **8180** (`ApiConfig.KEYCLOAK_URL`), so for a real phone also allow port 8180 in the firewall
(or `adb reverse tcp:8180 tcp:8180` with USB).

Each developer uses their own value. **Do not commit your personal IP**: before committing, check the diff of `ApiClient.kt` and put the default back.
The app uses plain `http` in development (`usesCleartextTraffic="true"` in the manifest).

## Keycloak (mobile login)

Keycloak is used by the **mobile app only** (teacher's requirement). The web app and the other services keep the existing JWT.

```
phone --(email + password)--> Keycloak :8180 --access token--> phone
phone --(Keycloak token)----> auth-service  POST /api/auth/keycloak --> SmartRecruit JWT --> phone
```

The SmartRecruit JWT is the one all services already accept, so only `auth-service` knows about Keycloak.
The role comes from MongoDB (the account with the same email). `KeycloakConfig.USE_KEYCLOAK = false` switches the app back to the old
login through `auth-service` only.

### 1. Run Keycloak (zip install, no Docker)

Download the zip from keycloak.org, extract it (for example `C:\keycloak\keycloak-26.x`), then in `cmd`:

```
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot
set KC_BOOTSTRAP_ADMIN_USERNAME=admin
set KC_BOOTSTRAP_ADMIN_PASSWORD=admin
cd C:\keycloak\keycloak-26.x
bin\kc.bat start-dev --http-port=8180 --http-host=0.0.0.0 --import-realm
```

(`--import-realm` loads `keycloak/smartrecruit-realm.json` from `data\import`, see *Read this first*. `JAVA_HOME` must be a full JDK 21. The admin variables are only needed the first time. Port 8180 because 8080 is often taken.)
Admin console: `http://localhost:8180`.

### 2. Realm configuration by hand (only if the import file is not used)

1. Create the realm **`smartrecruit`**.
2. Realm roles: **`CANDIDAT`**, **`RECRUTEUR`**, **`ADMIN`** (same names as the backend `Role` enum).
3. Client **`smartrecruit-mobile`**: Client authentication **OFF**, **Standard flow** and **Direct access grants** ON.
4. A test user: email as username, email, first name, last name, **Email verified ON**, a password with **Temporary OFF**, and the role `CANDIDAT`.
   First and last name are required, otherwise the password login fails with "Account is not fully set up".

### 3. Run `auth-service` with Keycloak enabled

Keycloak is **off by default** in `auth-service`, so the web flow is unchanged for everyone. To enable it, set these variables in the terminal that starts the service (PowerShell):

```powershell
$env:KEYCLOAK_ENABLED = "true"
$env:KEYCLOAK_ADMIN_USER = "admin"
$env:KEYCLOAK_ADMIN_PASSWORD = "admin"
.\mvnw.cmd spring-boot:run
```

`auth-service` uses the Keycloak master admin account to create, enable and delete users in Keycloak.
Never commit these values. They are only read from environment variables.

What `auth-service` does when enabled:

| Event | Keycloak action |
|-------|-----------------|
| Candidate registers | user created, enabled, role `CANDIDAT` |
| Recruiter sends a request | user created **disabled**, no role |
| Admin accepts the request | user enabled, role `RECRUTEUR` |
| Admin refuses the request | user deleted |
| Existing account logs in on the web (`/api/auth/login`) | copied to Keycloak if missing (the password is known at that moment) |

If Keycloak is stopped, registration and the web flow still work (a warning is logged), but the account cannot log in on mobile until it exists in Keycloak.
Mobile login does not refresh the Keycloak token: the SmartRecruit JWT lasts 24 hours, then the user logs in again.

## Architecture (MVVM)

```
Screen (Compose)  ->  ViewModel (UiState)  ->  Repository  ->  Retrofit API / DataStore
```

```
app/src/main/java/tn/smartrecruit/
├── MainActivity.kt, SmartRecruitApp.kt
├── data/
│   ├── model/        Role and domain models
│   ├── remote/       ApiClient (Retrofit + JWT interceptor), AuthApi, ApiError, dto/
│   ├── local/        TokenStore (DataStore: token, role, name)
│   └── repository/   AuthRepository, ProfileRepository
├── di/               AppContainer, ViewModelFactory (manual dependency injection)
└── ui/
    ├── theme/        colors and typography from the Figma design
    ├── components/   shared composables (AppTextField, PrimaryButton, ...)
    ├── navigation/   Routes, AppNavGraph
    └── screens/
        ├── auth/       splash, welcome, role choice, login, register, pending + ViewModels
        ├── candidate/  main scaffold (top menu, bottom bar with 5 tabs)
        ├── recruiter/  recruiter space
        └── profile/    profile screen and ViewModel (shared by both roles)
```

Libraries: Navigation Compose, Lifecycle ViewModel, DataStore, Retrofit + Gson, OkHttp logging, Coil, Material 3 and extended icons.

## Backend endpoints used

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/auth/login` | Login (web, and mobile when Keycloak is off), returns the JWT and the user |
| POST | `/api/auth/keycloak` | Mobile login: body `{ "accessToken" }` (Keycloak token), returns the SmartRecruit JWT and the user |
| POST | `/api/auth/register/candidat` | Create a candidate account |
| POST | `/api/auth/register/recruteur` | Send a recruiter request (account created after admin approval) |
| GET | `/api/auth/profil` | Profile of the logged-in user |
| PUT | `/api/auth/profil` | Update the profile |
| PUT | `/api/auth/mot-de-passe` | Change the password |

All other endpoints require `Authorization: Bearer <token>`, added automatically by the interceptor in `ApiClient`.
JSON field names are French and defined only in the `dto` package (`@SerializedName`).

## Troubleshooting

| Problem | Cause and fix |
|---------|---------------|
| Gradle error `jlink executable ... does not exist` | Gradle picked a JDK without `jlink` (for example the VS Code Java extension). Set the Gradle JVM version to 21 in the Gradle settings, then sync. |
| "Impossible de joindre le serveur" | Wrong `HOST` for your device, backend not running, or firewall. See *Backend address*. On a real phone `10.0.2.2` never works. |
| App closes right after launch | Invalid `HOST` format (missing `http://`, port or trailing slash included). Check Logcat for `FATAL EXCEPTION`. |
| "Validation échouée" | A JSON field name or a rule differs from the backend DTO (for example a password under 8 characters). Compare with the Java records in `auth-service` and see the Logcat `OkHttp` lines. |
| Greeting shows the email instead of the name | Old value stored on the phone. Log out and log in again. |
| Login says "Email ou mot de passe incorrect" but the account works in the web app | The user does not exist in Keycloak yet (Keycloak log shows `user_not_found`). Log in once on the web (or `POST /api/auth/login`) with `KEYCLOAK_ENABLED=true` so `auth-service` copies it. |
| Login says "Votre compte n'est pas encore activé" | Recruiter request still pending or refused (the Keycloak user is disabled). |
| "Jeton Keycloak invalide ou expiré" | The Keycloak token expired (5 min), or `auth-service` cannot reach Keycloak to get its public keys. Check Keycloak is running on 8180. |
| "Keycloak n'est pas activé sur ce serveur" (503) | Start `auth-service` with `KEYCLOAK_ENABLED=true`. |
| Keycloak fails with `Port already bound: 8080` | Another program uses 8080: start Keycloak with `--http-port=8180`. |
| Keycloak says `JAVA_HOME ... path doesn't exist` | Use the real folder of a JDK 21 (check `dir "C:\Program Files\Eclipse Adoptium"`). |
| `auth-service` logs `Keycloak: could not create user ... 401` | The admin credentials were not accepted. Check `KEYCLOAK_ADMIN_USER` / `KEYCLOAK_ADMIN_PASSWORD` in that terminal. |
| Phone cannot open `http://<PC IP>:8180` | Firewall rule for port 8180, same Wi-Fi, and Keycloak started with `--http-host=0.0.0.0`. |
| `adb` is not recognized | Use the full path `C:\Users\<you>\AppData\Local\Android\Sdk\platform-tools\adb`, or add that folder to `Path`. |

## Team

See the main README. Authentication, roles and user profile on mobile: Hassan Snoussi Jerbi.
