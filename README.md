# PetMood AI — Android App

Android app that lets a user record or upload a short video of their pet (always required), optionally
attach 2-3 supporting photos and a free-text behavior description, and send it all to a backend for
Gemini-based mood analysis — surfacing the result as a mood, confidence score, summary, and suggestions.

**Two repos, work together**: this repo is the Android frontend only. The backend lives in a sibling repo
at `../FastAPIProject` (own git history, own deploys) — not a submodule. Most features touch both; see
`CLAUDE.md` for the full architecture, key files, and known issues.

## Tech Stack
- Kotlin 2.2.10, Jetpack Compose (BOM 2024.09.00), MVVM
- Retrofit 2.12.0 + Moshi 1.15.2 for networking
- Coroutines / `StateFlow` for async + UI state
- Navigation Compose 2.8.9
- CameraX 1.5.0 for video capture (`camera-video` not yet wired up — see Known issues)
- Coil for image loading
- `minSdk` 24, `targetSdk`/`compileSdk` 36
- No DI framework — repositories/ViewModels are hand-wired in `MainActivity.onCreate()`

## Architecture
```
Android app (Retrofit/Moshi, JWT in SessionManager/SharedPreferences)
   → FastAPI backend on Azure App Service "PetmoodAI-AU" (australiaeast)
       → Azure Postgres Flexible Server
       → Google Gemini API (mood analysis)
```
- The API base URL is hardcoded in `MainActivity.kt` to the live backend
  (`https://petmoodai-au.azurewebsites.net`) — there's no separate local/staging config yet.
- Scan flow is async: the app uploads video (+ optional photos/description) in one request, then polls
  `GET /pets/{id}/scans/{scan_id}` every 2.5s (30 attempts / 75s ceiling) until the backend finishes
  analysis via a background Gemini call.
- The app supports exactly **one pet per account** today — no pet-selection UI exists.

## Run locally
**Prerequisites**: [Android Studio](https://developer.android.com/studio), the sibling backend repo
(`../FastAPIProject`) running or reachable (see its README).

1. Open Android Studio, select **Open**, and choose this project's directory.
2. Let Android Studio sync/import and resolve Gradle dependencies.
3. Run the app on an emulator or physical device (`Run ▸ app`).

Command-line equivalents (from repo root):
- Compile check (fast): `./gradlew :app:compileDebugKotlin`
- Full build: `./gradlew build`
- Unit tests: `./gradlew test` — mostly template boilerplate today; no real app test coverage yet
- Instrumented tests (needs device/emulator): `./gradlew connectedAndroidTest`

> Note: `.env` / `GEMINI_API_KEY` and the `secrets` Gradle plugin are leftovers from this project's
> original AI-Studio scaffold and are unused by the app today — Gemini is called server-side by the
> backend, not from the client. Safe to ignore for local dev.

## Known limitations
- Live camera recording (record button on the scanner screen) is currently a mock and does not capture
  real video — only the gallery-picker upload path works end-to-end.
- ViewModels don't survive configuration changes (e.g. screen rotation) yet.
- App namespace/`applicationId` is still the template default `com.example` — needs renaming before any
  release build.

See `CLAUDE.md` for the full list of known issues, key files, and conventions.
