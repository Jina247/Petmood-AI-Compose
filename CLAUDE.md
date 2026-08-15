# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

PetMood AI: an Android app that lets a user record or upload a short video of their pet (always
required), optionally attach 2-3 supporting photos and a free-text behavior description, sends it all
together to a backend for Gemini-based mood analysis, and shows the result (mood, confidence, summary,
suggestions).

**Two repos, work together**: this repo is the Android frontend only. The backend lives in a sibling repo
at `/Users/jinutran/Projects/FastAPIProject` (own git history, own deploys) — not a submodule, just a
sibling directory. Most features touch both.

## Tech stack

**Android** (this repo): Kotlin 2.2.10, Jetpack Compose (BOM 2024.09.00), MVVM. Retrofit 2.12.0 +
**Moshi** 1.15.2 for networking (not Gson — see Known issues). Coroutines/`StateFlow` for async + UI
state. Navigation Compose 2.8.9. CameraX 1.5.0 (`camera2`/`core`/`lifecycle`/`view` — `camera-video` not
yet added). Coil for images. `minSdk 24`, `targetSdk`/`compileSdk 36`. No DI framework — repositories/
ViewModels are hand-wired in `MainActivity.onCreate()` (see Known issues, this is also the rotation bug).

**Backend** (`../FastAPIProject`): FastAPI 0.136.3, SQLAlchemy 2.0.50 + Alembic 1.18.5, **PostgreSQL**
(via `psycopg2-binary`) — not SQLite (see Known issues), `python-jose` for JWT, `pwdlib` for password
hashing, `google-genai` 2.16.0 for Gemini. Python 3.11 (Azure runtime) / 3.13 (local `.venv`).

## Architecture

```
Android app (Retrofit/Moshi, JWT in SessionManager/SharedPreferences)
   → FastAPI on Azure App Service "PetmoodAI-AU", resource group petmood-rg, region australiaeast
       → Azure Postgres Flexible Server "petmoodai-pg" (db "petmood")
       → Google Gemini API (google-genai, model gemini-flash-latest) — called from a FastAPI
         BackgroundTask after scan upload; NOT synchronous with the upload request
```

- Deploy: GitHub Actions (`.github/workflows/main_petmoodai_au.yml`) on push to `main`, publish-profile
  auth, `SCM_DO_BUILD_DURING_DEPLOYMENT=true` (Oryx build on the App Service itself).
- `main.py`'s `lifespan` runs `alembic upgrade head` on **every app startup** — schema migrations are
  not a separate manual deploy step.
- Scan flow is async: `POST /pets/{id}/scans` returns `status: "processing"` immediately; a background
  task uploads the video to Gemini, polls until it's `ACTIVE`, and writes `mood_result`/`confidence`/
  `summary`/`suggestions`/`error_message` back onto the `Scan` row. Client polls `GET /pets/{id}/scans/{scan_id}`.
- A scan's video is always required; photos (0-3) and a text description are optional extras attached to
  the *same* `POST /pets/{id}/scans` request (multipart fields `file`/`photos`/`description`) — **not** a
  separate endpoint or flow. `gemini_client.analyze_pet_scan()` is the one entry point: it combines all
  three into one Gemini call (video via Files API, photos inline as bytes, description folded into the
  prompt) and always asks for `suggestions` (actionable next steps for the owner) alongside mood/
  confidence/summary, whether or not photos/description were attached.
- Android's `ScanViewModel` polls every 2.5s, 30 attempts (75s hard ceiling), then surfaces a timeout
  `Error` state rather than polling forever.
- The app supports exactly **one pet per account** today — no pet-selection UI exists. Don't add a
  `currentPetId`-style side channel; resolve the pet directly from `ScanViewModel.petProfile`.

## Key files

**Android**
- `MainActivity.kt` — builds the single Retrofit/Moshi `ApiService`, constructs all repositories/
  ViewModels, owns the entire `NavHost` (all screen routes and their transitions live here, not in the
  screens themselves).
- `viewmodel/ScanViewModel.kt` — upload + poll state machine (`ScanUiState`: Idle/Uploading/Analysing/
  Success/Error); also owns `petProfile` (must be explicitly refreshed via `refreshPetProfile()`, see
  Known issues) and the optional scan extras (`selectedPhotos`/`description`, mutated via
  `onPhotosPicked`/`removePhoto`/`updateDescription`, capped at `MAX_PHOTOS` = 3) — snapshotted and
  cleared at the start of each submission so the next scan starts blank regardless of outcome.
- `ui/screens/ScannerScreen.kt` — record/pick video (unchanged: still auto-uploads on select, no review
  step) plus the optional supporting-photos thumbnail row (add/remove/tap-to-zoom) and description field.
  This is the only place users add photos/description — there's no separate screen for it.
- `viewmodel/{Auth,Pet,History}ViewModel.kt` — one sealed `*UiState` per screen area, each own
  `StateFlow`.
- `session/SessionManager.kt` — JWT + user info in `SharedPreferences`; `init(context)` must run before
  first use (called in `MainActivity.onCreate`).
- `data/api/ApiService.kt` — single Retrofit interface for all endpoints (auth/pets/scans) + the request/
  response DTOs for auth. `data/api/AuthInterceptor.kt` attaches the bearer token and force-logs-out on
  401.
- `data/repository/{Auth,Pet,Scan}Repository.kt` — thin wrappers; `ScanRepository` returns `Result<T>`,
  the other two mostly let exceptions propagate (inconsistent on purpose vs. by design — see Known
  issues if asked to add a new repository method).
- `data/model/{PetProfile,ScanResult}.kt` — Moshi `@Json`-annotated models; `ScanResult` doubles as both
  the wire model and the UI model (renamed fields like `mood`/`timestamp` vs. backend's `mood_result`/
  `created_at`); also carries `description`/`suggestions` (both nullable — absent on older responses).

**Backend** (`../FastAPIProject`)
- `main.py` — app + lifespan (runs migrations, warns if `GEMINI_API_KEY` unset).
- `config.py` — all env vars: `SECRET_KEY`, `DATABASE_URL` (sqlite fallback for local dev only),
  `GEMINI_API_KEY`. `ALGORITHM` is hardcoded `"HS256"`, **not** an env var, despite looking like one.
- `routers/auth.py` — `/auth/register`, `/auth/login`, `/auth/users/me` (note the prefix — see Known
  issues), JWT issuance.
- `routers/pets.py` — CRUD, owner-scoped (`Pet.owner_id == current_user.id`) on every query.
- `routers/scans.py` — single `create_scan` handler validates the required video (content-type allowlist,
  50MB cap) plus optional `photos` (≤3, content-type allowlist, 4MB/photo cap) and `description` (≤1000
  chars), shared 5/hour rate limit, kicks off the background `analyze_scan` task.
- `gemini_client.py` — all Gemini interaction; `analyze_pet_scan(video_path, photo_paths, description)` is
  the one entry point (no separate video-only/photo-only functions) — raises `GeminiAnalysisError` (caught
  in `scans.py`'s background task and written to `Scan.error_message`).
- `database/models.py` — `User`/`Pet`/`Scan`, UUID string PKs, native Postgres enums for `gender` and
  `status`. `Scan` also has nullable `description` (str), `photo_paths` (JSON list), `suggestions` (JSON
  list) columns — no `media_type`/discriminator column; a scan's "kind" is just whatever's populated.
- `dependencies.py` — `get_current_user` (JWT bearer decode + DB lookup).
- `alembic/versions/` — 3 migrations so far; baseline schema, `error_message`/`updated_at` on scans, and
  `description`/`photo_paths`/`suggestions` on scans.

## Commands

**Android** (from repo root):
- Compile check (fast): `./gradlew :app:compileDebugKotlin`
- Full build: `./gradlew build`
- Unit tests: `./gradlew test` — currently just template boilerplate (`ExampleUnitTest`,
  `ExampleRobolectricTest`, `GreetingScreenshotTest`), no real app tests exist yet.
- Instrumented tests (needs device/emulator): `./gradlew connectedAndroidTest`

**Backend** (from `../FastAPIProject`, `.venv` already present):
- Run locally: `.venv/bin/uvicorn main:app --reload` (matches Azure's own start command:
  `uvicorn main:app --host 0.0.0.0 --port 8000`)
- Install deps: `.venv/bin/pip install -r requirements.txt`
- Run migrations manually: `.venv/bin/python -m alembic upgrade head` (also runs automatically on every
  app startup, see Architecture)
- New migration: `.venv/bin/python -m alembic revision --autogenerate -m "message"`
- No test suite exists (`test_*.py` search comes up empty) — don't assume `pytest` is configured.

## Known issues / gotchas already fixed

- **Azure `SECRET_KEY` App Setting was misspelled `SECRECT_KEY`** → `os.getenv("SECRET_KEY")` was `None`
  → every login 500'd (`jose.exceptions.JWSError`). Fixed by renaming the App Setting.
- **SQLite lived in App Service's ephemeral per-deployment directory** → every restart wiped all users/
  pets/scans. Resolved by migrating fully to Postgres (see below), not just by relocating the file.
- **Two parallel Retrofit clients**: a dead Gson-based `RetrofitInstance` (used only by `AuthRepository`,
  no `AuthInterceptor` attached) vs. the real Moshi-based client built in `MainActivity`. Deleted;
  everything now shares one Moshi client.
- **`ScanViewModel.petProfile` got stuck at a cached `null` forever** — was a one-shot `flow{}` piped
  through `stateIn(WhileSubscribed(5000))`, but `MainActivity` holds a permanent top-level collector on
  it from app launch (before login/pet-creation), so the subscriber count never hits zero and the fetch
  never re-ran. Now an explicitly-refreshed `MutableStateFlow` (`refreshPetProfile()`, called on login
  and after pet save). If you add new state that screens read before the user is fully set up, don't use
  the `stateIn(WhileSubscribed)` one-shot pattern for it.
- **`ScanRepository.uploadScan` hardcoded `Content-Type: video/mp4`** regardless of the actual file —
  now takes a real detected MIME type.
- **Backend was deployed in Azure `eastasia` (Hong Kong)** — not a supported Gemini API region
  (`400 FAILED_PRECONDITION: User location is not supported`). Migrated to `australiaeast`; old
  resource group `petmood-rg1` fully deleted.
- **`SettingsScreen`'s pet age**: `petProfile?.age ?: "2 years"` Elvis'd an `Int` with a `String` —
  compiles (common supertype inference), but rendered real pets' age as a bare number with no unit.
- **Photos/description are extras on the video scan, not an alternate input mode**: an earlier draft of
  this feature built a separate `POST /pets/{id}/scans/photos` endpoint plus a parallel
  `PhotoScanViewModel`/`PhotoScanScreen` and dual-flow nav (`activeScanFlow` tracking which ViewModel was
  "active" for the shared `AnalysingScreen`/`ResultsScreen`) before being corrected. Video is always
  required; photos (0-3) and description ride along in the *same* request/screen as the video — see
  Architecture. If asked to extend scan inputs further, extend `create_scan`/`ScanViewModel`/
  `ScannerScreen` in place; don't reach for a second endpoint/screen/ViewModel.
- **Gemini free-tier daily quota is easy to exhaust while iterating**: `generate_content` is capped at 20
  requests/day on the free tier — confirmed via live `429 RESOURCE_EXHAUSTED` responses, which look
  different from (and are easy to confuse with) the transient `503 UNAVAILABLE` "high demand" responses
  that also show up under load. Once the daily cap is hit, every scan fails with the same
  `GeminiAnalysisError` message regardless of input — check https://ai.dev/rate-limit before assuming a
  code regression if many scans in a row fail identically.

### Currently open

- **`/users/me` path mismatch**: `ApiService.kt`'s `getCurrentUserInfo()` calls `@GET("users/me")`
  (relative → `{baseUrl}/users/me`), but the backend route is registered under the `/auth`-prefixed
  router, so it's actually `/auth/users/me`. Confirmed live: `/users/me` → 404, `/auth/users/me` → 200.
  Since `AuthRepository.login()`/`.register()` call `getCurrentUserInfo()` immediately after
  login/register and wrap both calls in one try/catch, **this 404 currently makes login/register always
  return `Result.failure`**, even though the account and token are created successfully. One-line fix:
  `@GET("auth/users/me")`.
- **ViewModels don't survive configuration changes**: `MainActivity.onCreate()` constructs
  `ScanViewModel`/etc. with plain constructors instead of `ViewModelProvider`/`viewModel()`. Rotation
  destroys/recreates the Activity, which creates a brand-new `ScanViewModel` from scratch, orphaning any
  in-flight scan poll (it keeps running against the old, now-unobserved instance — the result lands in
  History but the UI never reflects it, `AnalysingScreen`'s progress bar sticks). Needs a proper
  `ViewModelProvider.Factory` + `viewModel()` composable usage; scoped but not yet implemented.
- Live camera recording (`ScannerScreen`'s record button) is still a mock — creates an empty 0-byte file
  and uploads it, which Gemini correctly rejects. Only the gallery-picker path uploads real video.
  `camera-video` dependency not yet added.

## Conventions

- Kotlin package-by-layer: `data.api` / `data.model` / `data.repository` / `viewmodel` / `ui.screens` /
  `ui.components` / `session`. App namespace/applicationId is still the template default `com.example`
  (unchanged from scaffold — rename before any release build).
- Python: `schemas/` (Pydantic, wire format) kept separate from `database/models.py` (SQLAlchemy, DB
  format); routers grouped by resource under `routers/`.
- Commit messages are mixed in style historically (`Feat: ...`, `Fix: ...`, `Add: ...`, terse one-liners
  like `"Commit"`); recent work uses a `Subject line` + bullet body + `Co-Authored-By: Claude Sonnet 5
  <noreply@anthropic.com>` trailer for Claude-assisted commits — follow that for new commits here.
- Backend git history shows a feature-branch + PR pattern (`jinas-branch`, `feature/user-me-endpoint`)
  merged into `main`; recent backend work (this migration) was committed directly to `main` instead —
  ask before assuming which is wanted for new work.
- Both repos' `README.md` are stale (Android's still describes an old AI-Studio scaffold with a
  client-side `GEMINI_API_KEY`; backend's still says "Deployed on Railway") — don't trust them over this
  file or the actual code.
