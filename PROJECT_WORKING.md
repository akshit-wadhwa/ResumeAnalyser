# ResumeAnalyser - Detailed Working Document

This document explains how the project works internally, based on the current implementation. It is intentionally split into two major sections: **Frontend** and **Backend**.

---

## Frontend

The frontend is a **JavaFX desktop client** (module: `frontend`) responsible for user interaction, file selection, view navigation, and communication with backend REST APIs.

### 1. Frontend Architecture

The frontend follows a layered structure:

- **Entry layer**: launches JavaFX app and initial scene.
- **Navigation/state layer**: global navigation and app session state.
- **Controller layer**: one controller per FXML screen.
- **Service layer**: HTTP calls to backend.
- **Model layer**: DTO-like Java objects for API serialization/deserialization.

#### Key classes

- `MainApp`: application bootstrap.
- `ViewNavigator`: centralized screen switching.
- `AppState`: in-memory cross-screen shared state.
- `ApiClient`: all backend API calls using `HttpClient`.

### 2. Startup and Screen Loading

When the frontend starts:

1. `MainApp.start()` sets the shared `Stage` into `ViewNavigator`.
2. It loads `/fxml/login.fxml` as first screen.
3. It applies shared stylesheet `/css/app.css`.

Navigation between screens is done through `ViewNavigator.navigate("/fxml/...")`, which replaces the root node of the existing scene (or creates one if needed).

### 3. Global Runtime State (`AppState`)

`AppState` is a static holder used by multiple controllers. It stores:

- current authenticated `UserSession`
- current user password (used for backend calls requiring credentials)
- latest analysis id (`lastAnalysisId`)
- latest completed analysis result (`lastResult`)
- latest job text

This allows controllers to remain loosely coupled while still sharing runtime data.

### 4. API Communication (`ApiClient`)

`ApiClient` is configured with backend base URL `http://localhost:8081` and uses Jackson (`ObjectMapper`) for JSON mapping.

Implemented operations:

- `login(email, password)` → `POST /auth/login`
- `register(email, password, fullName)` → `POST /auth/register`
- `startAnalysis(...)` → `POST /analysis/analyze` (multipart)
- `pollStatus(analysisId)` → `GET /analysis/status/{analysisId}`
- `rankResumes(...)` → `POST /comparison/rank` (multipart)
- `fetchAdminSummary()` → `GET /admin/summary`
- `downloadReport(analysisId, file)` → `GET /analysis/{analysisId}/report`
- `getHistory(email, password)` → `GET /history?userEmail=...&userPassword=...`

#### Multipart handling

For file upload endpoints, `MultipartBodyBuilder` builds request body manually with boundary and file/text parts.

### 5. Screen-by-Screen Working

#### A) Login screen (`LoginController`)

- Validates email/password not blank.
- Runs login call in JavaFX `Task` (background thread).
- On success:
  - stores session and password in `AppState`
  - navigates to upload screen.
- On failure:
  - shows status message (`Invalid email or password` mapping for common case).

#### B) Register screen (`RegisterController`)

- Validates:
  - email present + contains `@`
  - full name present
  - password length >= 6
  - confirm password matches
- Calls register API in background `Task`.
- On success, stores session/password and opens upload screen.

#### C) Upload + analysis start (`UploadController`)

Capabilities:

- Pick resume file (`.pdf`, `.docx`).
- Pick optional job file (`.pdf`, `.docx`).
- Enter job text manually.
- Drag-and-drop resume into drop zone.

Validation before analysis:

- resume is mandatory
- at least one of job file or job text is required

Then it calls `startAnalysis()`. On success:

- stores analysis id in `AppState.lastAnalysisId`
- navigates to analyzing screen.

Also supports multi-resume comparison:

- collect multiple resume files in a list
- requires job text
- calls `rankResumes()`
- converts typed model objects into map list expected by `ComparisonController` and switches screen.

Additional actions:

- open admin dashboard
- open history view
- logout (clear session/password and navigate to login).

#### D) Analyzing screen (`AnalyzingController`)

- Starts indeterminate progress indicator.
- Starts polling timeline every 2 seconds.
- Each poll calls `pollStatus(lastAnalysisId)` in background task.

Behavior by status:

- `RUNNING` → no navigation yet.
- `COMPLETED` → saves result in `AppState.lastResult`, stops poller, navigates to results.
- `FAILED` / exception → shows error and stops poller.

#### E) Results screen (`ResultsController`)

On initialize:

- fetches `AppState.lastResult`
- redirects to upload if no result exists.
- renders:
  - match score
  - confidence score
  - seniority
  - matched skills
  - missing skills
  - weaknesses
  - recommendations
- animates score progress bar.

Export flow:

- user chooses output `.pdf` destination
- controller calls `downloadReport(lastAnalysisId, target)` in background thread.

#### F) Comparison screen (`ComparisonController`)

- receives a list of result maps via `setResults(...)`.
- sorts by descending match score.
- renders card per resume with:
  - rank
  - filename
  - score + confidence
  - skill matched/missing counts
- uses rank-based border coloring (top candidate emphasized).

#### G) History screen (`HistoryController`)

- validates user session presence.
- fetches analysis history for logged-in user.
- populates table with:
  - score
  - confidence
  - top matched skills
  - top missing skills
  - formatted created date
- supports refresh and back navigation.

#### H) Admin screen (`AdminController`)

- calls `/admin/summary`.
- displays total users, total analyses, average match score.

### 6. Frontend Concurrency and UX Notes

- Network calls are wrapped in `Task` and run on new threads to avoid UI freeze.
- Controllers use small transitions (fade/progress animations) for smoother UX.
- Error handling is mostly message-driven (label updates).

### 7. Frontend Data Models

Key model classes mirror backend DTO responses:

- `UserSession`: user id, role, token, email.
- `AnalysisResult`: detailed analysis payload + processing steps + createdAt.
- `RankedResume`: filename, match/confidence, matched/missing skills.
- `AdminSummary`: totals and average score.

---

## Backend

The backend is a **Spring Boot + JDBC API** (module: `backend`) that performs authentication, parsing, ML analysis orchestration, persistence, report generation, and data retrieval.

### 1. Backend Architecture

Main layers:

- **Controllers**: REST endpoints.
- **Services**: business workflows.
- **Repositories**: SQL access via `JdbcTemplate`.
- **Config**: app beans (executor, cache/tracker, rest template).
- **Model/DTO**: persistence models + API transport objects.

App bootstrap:

- `ResumeAnalyserApplication` with `@SpringBootApplication` and `@EnableAsync`.

### 2. Configuration and Runtime Beans

From `application.yml`:

- server port: `8081`
- PostgreSQL datasource
- schema init enabled (`schema.sql`)
- ML endpoint (default `http://localhost:8000/analyze`)
- ML timeouts
- cache TTL (`app.cache.ttl-seconds`, default in repo: 900 seconds)

Configured beans:

- `RestTemplate` with connect/read timeout (`AppConfig`)
- `analysisExecutor` thread pool (`AsyncConfig`): core 4, max 8, queue 50
- `CacheService` and `AnalysisTracker` (`StateConfig`)

### 3. REST API Endpoints and Behavior

#### Authentication (`AuthController`)

- `POST /auth/login`
  - validates credentials via `AuthService.login`
  - returns `LoginResponse(userId, role, token)`
- `POST /auth/register`
  - creates user via `AuthService.register`
  - returns `LoginResponse` with generated token

Token is currently generated as SHA-256 of email + timestamp and returned to client; it is not used as full JWT auth enforcement across all routes.

#### Analysis (`AnalysisController`)

- `POST /analysis/analyze` (multipart)
  - inputs: resume, optional jobFile, optional jobText, userEmail, userPassword
  - authenticates using `AuthService.login`
  - starts async analysis via `AnalysisService.startAnalysis`
  - returns `analysisId`

- `GET /analysis/status/{analysisId}`
  - reads current `AnalysisState`
  - returns status (`RUNNING`/`COMPLETED`/`FAILED`) plus result/error

- `GET /analysis/{analysisId}/report`
  - generates PDF report from completed result using `ReportService`

#### Comparison (`ComparisonController`)

- `POST /comparison/rank` (multipart)
  - inputs: list of resume files + jobText + credentials
  - uses `AuthService.loginOrCreate`
  - returns ranked resume list from `ComparisonService`

#### History (`HistoryController` and `UserHistoryController`)

- `GET /history?userEmail=...&userPassword=...`
  - authenticates with login
  - returns last analyses for user

- `GET /history/{userId}`
  - fetches by user id (basic validation, optional token header)

#### Admin (`AdminController`)

- `GET /admin/summary`
  - returns total users, total analyses, average match score.

### 4. Core Analysis Workflow (`AnalysisService`)

`AnalysisService` is the primary orchestrator.

#### Start phase

`startAnalysis(...)`:

1. Generates UUID `analysisId`.
2. Tracker state initialized to RUNNING.
3. Immediately reads multipart file bytes in request thread (important to avoid temp file cleanup race).
4. Submits async task to `analysisExecutor`.

#### Async execution phase (`runAnalysis`)

Pipeline steps:

1. **Parse resume** using `ParsingService`.
2. **Parse job**
   - uses text input if provided,
   - else parses uploaded job file,
   - fails if both are empty.
3. **Cache lookup**
   - cache key = SHA-256(`resumeText + "|" + jobContent`)
   - if cache hit: return cached result quickly as COMPLETED.
4. **ML analysis**
   - call Python ML endpoint through `MLClient`.
5. **Persist data**
   - save resume row
   - save job description row
   - save analysis result row with JSON fields.
6. **Build DTO + cache put**
   - create `AnalysisResultDto`
   - store in in-memory cache.
7. **Update tracker** to COMPLETED.

On any exception:

- tracker state set to FAILED
- error message attached
- detailed backend log emitted.

### 5. Parsing Layer (`core` module via `ParsingService`)

Backend delegates document parsing to shared `core` module.

- Type detection by filename extension (`.docx` vs default `.pdf`).
- `PdfParser`: Apache PDFBox text extraction.
- `DocxParser`: Apache POI paragraph extraction.
- `TextCleaner` normalization:
  - HTML unescape
  - normalize whitespace/newlines
  - remove non-alphanumeric symbols
  - lowercase conversion

This ensures ML input text is cleaned and consistent.

### 6. ML Integration (`MLClient` + Python service)

`MLClient` posts JSON:

- `resume_text`
- `job_text`

to configured ML endpoint and maps response into `MLResponse`.

Expected response fields:

- scores (match, confidence)
- matched/missing skills
- weaknesses, recommendations
- experience years, education, seniority

#### Python service behavior (module `ml-service`)

The FastAPI service:

- normalizes texts
- loads/caches skills list from `data/skills.txt`
- extracts skills and frequencies
- computes TF-IDF cosine similarity
- extracts experience and education heuristics
- combines weighted factors into ATS-like score
- builds weaknesses and recommendations

This output directly drives frontend result views.

### 7. Persistence and Database Design

Schema creates:

- `users`
- `resumes`
- `job_descriptions`
- `analysis_results`
- `comparisons`

Repository responsibilities:

- `UserRepository`: user lookup/create/count.
- `ResumeRepository`: insert resume documents.
- `JobRepository`: insert job descriptions.
- `AnalysisResultRepository`:
  - store analysis row,
  - parse JSON text columns back to Java lists,
  - list user history,
  - aggregate totals and averages.
- `ComparisonRepository`: save comparison ranking payload.

JSON-heavy fields (`matched_skills`, `missing_skills`, `weaknesses`, `recommendations`, `processing_steps`, comparison ranking) are serialized as text JSON in PostgreSQL.

### 8. Comparison Workflow (`ComparisonService`)

`compare(user, resumes, jobText)` flow:

1. Save job description row.
2. For each resume:
   - parse text
   - save resume row
   - call ML analyze against same job text
   - create `RankedResumeDto`.
3. Sort ranking by match score descending.
4. Persist comparison summary in `comparisons` table.
5. Return ranking to client.

### 9. Report Generation (`ReportService`)

PDF generation uses Apache PDFBox and writes:

- title
- match/confidence/seniority summary
- matched skills list
- missing skills list
- recommendations list

Generated bytes are returned as downloadable `application/pdf`.

### 10. Caching, Status Tracking, and Async State

#### `CacheService`

- in-memory `ConcurrentHashMap`
- TTL-based eviction on read
- avoids repeated ML calls for same resume/job text combination.

#### `AnalysisTracker`

- in-memory analysis status registry keyed by analysisId
- used by polling endpoint `/analysis/status/{analysisId}`.

#### `AnalysisState`

Contains:

- current status enum
- optional completed result
- optional error message.

### 11. Security and Validation Notes

Current implementation includes basic auth and validation:

- password hashing with SHA-256 before persistence/comparison
- credential checks for login-protected operations
- request validation for certain DTOs

Important implementation note:

- API token is generated and returned, but most routes still rely on email/password parameters rather than strict token middleware.

### 12. End-to-End Data Flow Summary

1. User logs in/registers from JavaFX frontend.
2. Frontend uploads resume + job context to backend.
3. Backend authenticates user and starts async analysis.
4. Backend parses documents via `core`, checks cache, calls ML service, stores result.
5. Frontend polls status until complete.
6. Result is rendered in UI, can be exported as PDF, appears in history, and contributes to admin metrics.
7. Multi-resume mode sends batch resumes and receives ordered ranking.

---

This document reflects the current repository behavior and API/UI wiring as implemented.
