# Retrivox — Project Documentation & Development Roadmap

> **Purpose:** This is the source-of-truth project guide for developers and AI coding agents working on Retrivox. Read this file before making changes. Update it when architecture, APIs, implementation status, or priorities change.
>
> **Status note:** Items marked **Verified** are based on results reported during development. Items marked **Planned** or **Needs verification** must not be treated as implemented.

**Last updated:** 23 October 2026  
**Project:** Retrivox — AI-Powered Study & Revision Assistant  
**Tagline:** Your Notes. Your Knowledge. Your Next Level.  
**Description:** An AI-powered study and revision assistant that transforms uploaded PDFs and lecture notes into summaries, practice quizzes, and document-grounded question answering.  
**Repository root:** `D:\StudySync`  
**Backend folder:** `D:\StudySync\backend`

---

## 1. Product vision

Retrivox helps students and researchers study from their own notes and documents. The intended workflow is:

1. Upload study material, initially PDFs.
2. Extract readable text from the uploaded document.
3. Generate concise summaries.
4. Generate practice questions and MCQs from the material.
5. Ask questions and receive answers grounded in the uploaded material.
6. Track quiz attempts and learning progress.

Build a reliable backend and validate each milestone before adding the next. Prefer small, testable changes over broad rewrites.

## 2. Product principles

- **Ground answers in source material:** AI answers should use the student's uploaded content where possible and should not invent document facts.
- **Incremental delivery:** Finish and verify one milestone before starting the next.
- **Preserve working functionality:** Avoid changing unrelated code, endpoints, configuration, or dependencies.
- **Evidence over claims:** A feature is complete only when its behavior has been tested. A successful compilation alone is not an end-to-end test.
- **Secure configuration:** Keep database passwords, API keys, tokens, and other secrets in environment variables. Never commit secrets or print them in logs.
- **Clear failure handling:** Validate input and return useful HTTP errors without exposing stack traces or secrets to users.
- **Cost awareness:** Keep AI requests focused, avoid unnecessary context, and use the lowest reasoning/model setting that reliably completes the task.

## 3. Current technology stack

- Java 21
- Spring Boot 4.1.1
- Spring Security 7.x (Stateless Bearer JWT authentication & BCrypt password hashing)
- JJWT 0.12.6
- Maven Wrapper
- Spring Data JPA
- PostgreSQL hosted on Supabase
- Apache PDFBox 3.0.2 for PDF text extraction

### Planned / not yet confirmed

- Frontend integration
- CI/CD Deployment pipelines

**Important:** Do not assume a planned technology has already been added. Inspect the current repository and `pom.xml` before making implementation decisions.

## 4. Environment and configuration

The backend uses environment variables for database configuration:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

The reported Supabase connection uses the PostgreSQL Session Pooler. Do not put the real password or other secrets in this document, source control, prompts, logs, or screenshots.

Expected JDBC URL pattern (use the actual configured host from the developer's environment):

```text
jdbc:postgresql://<pooler-host>:5432/postgres?sslmode=require
```

Do not replace the current working database configuration without first understanding why a change is necessary.

### Local commands (PowerShell)

Run these commands from `D:\StudySync\backend`.

```powershell
# Compile/package and execute the configured test phase
.\mvnw.cmd clean package

# Compile tests only (does not run tests)
.\mvnw.cmd test-compile

# Start the Spring Boot application
.\mvnw.cmd spring-boot:run
```

The generated executable JAR has been reported at:

```text
target\studysync-0.0.1-SNAPSHOT.jar
```

## 5. Verified baseline and caveats

The following results were reported during development:

| Check | Reported result | Interpretation |
|---|---|---|
| `GET /api/health` | `200 OK`, `{"application":"StudySync","status":"UP"}` | Health endpoint responds successfully |
| `GET /api/db-check` | `200 OK`, `{"application":"StudySync","database":"CONNECTED"}` | Database connectivity was verified at that time |
| `GET /api/study-materials` | `200 OK`, returned active database records | Listing was verified |
| `POST /api/study-materials` | `201 CREATED`, created a record titled `Calculus Notes` | Create was verified |
| `.\mvnw.cmd test-compile` | `BUILD SUCCESS` | Test sources compiled; this does not mean tests ran |
| `.\mvnw.cmd clean package` | `BUILD SUCCESS`; one test skipped | Packaging succeeded, but `StudysyncApplicationTests` was disabled because it requires an external database |

### Important testing caveats

- The test `StudysyncApplicationTests.java` was reportedly annotated with `@Disabled("Requires external database connection")`. Inspect the current file to confirm this remains true.
- A disabled test is skipped, not passed. Do not report it as a successful application-context test.
- `clean package` is the primary build command, but inspect the test summary to see whether tests actually ran, failed, or were skipped.
- The `/api/db-test` alias was reportedly added alongside `/api/db-check` in `DatabaseController.java`. Restart the application before verifying the new mapping, and test `/api/db-test` directly. Do not claim that route is verified unless it returns the expected response.
- Update and delete CRUD behavior still need explicit verification unless current test evidence proves otherwise.
- PDF upload/text extraction needs a real end-to-end test. Do not assume it is complete solely because the PDFBox compilation error was corrected.

## 6. Current API inventory

These routes have been reported in the project. Confirm the actual mappings in the code before changing them.

| Method | Route | Current status | Notes |
|---|---|---|---|
| GET | `/api/health` | Verified | Public: Returns application status (`{"status":"UP","application":"StudySync"}`) |
| GET | `/api/db-check` | Verified | Public: Checks database connectivity (`{"database":"CONNECTED"}`) |
| GET | `/api/db-test` | Verified | Public: Verified alias to `/api/db-check` returning 200 OK |
| POST | `/api/auth/register` | Verified | Public: Register account (email, password, fullName). Returns 201 Created with JWT + profile |
| POST | `/api/auth/login` | Verified | Public: Authenticate user. Returns 200 OK with Bearer JWT token + profile (401 on bad credentials) |
| GET | `/api/auth/me` | Verified | Protected: Returns current authenticated user's profile |
| GET | `/api/study-materials` | Verified | Protected: Lists ONLY current authenticated user's study materials |
| POST | `/api/study-materials` | Verified | Protected: Creates study material bound to authenticated owner (201 Created) |
| GET | `/api/study-materials/{id}` | Verified | Protected: Fetches material by ID (200 OK for owner, 403 Forbidden for cross-user, 404 missing) |
| DELETE | `/api/study-materials/{id}` | Verified | Protected: Deletes material by ID (204 No Content for owner, 403 Forbidden for cross-user, 404 missing) |
| POST | `/api/study-materials/{id}/upload` | Verified | Protected: Uploads PDF, extracts text; enforces ownership checks (403 for non-owner) |
| POST | `/api/study-materials/{id}/summary` | Verified | Protected: Generates AI summary; enforces ownership checks (403 for non-owner) |
| GET | `/api/study-materials/{id}/summary` | Verified | Protected: Retrieves saved summary for material (403 for non-owner, 404 not generated) |
| POST | `/api/study-materials/{id}/quizzes` | Verified | Protected: Generates MCQs bound to user; enforces ownership checks (403 for non-owner) |
| GET | `/api/quizzes/{quizId}` | Verified | Protected: Retrieves quiz test; enforces ownership checks (403 for non-owner, 404 missing) |
| POST | `/api/quizzes/{quizId}/submit` | Verified | Protected: Submits answers, grades, and persists `QuizAttempt` for current user (403 for non-owner) |
| GET | `/api/quizzes/attempts` | Verified | Protected: Retrieves all past quiz attempts for current user |
| POST | `/api/study-materials/{id}/ask` | Verified | Protected: RAG Q&A grounded in material chunks; enforces ownership checks (403 for non-owner) |

Do not invent API routes. If a route is added or changed, update this table with its exact method, path, request format, response, and test status.

## 7. Current architecture (known)

Known backend components include:

- `com.studysync.StudysyncApplication` — Spring Boot application entry point.
- `DatabaseController.java` — health/database-related endpoint mappings (verify exact responsibilities).
- `StudyMaterial.java` — study-material entity (verify actual fields and annotations).
- `StudyMaterialRepository.java` — Spring Data JPA repository (verify current interface).
- `StudyMaterialController.java` — study-material API and PDF handling (verify current responsibilities).
- `StudysyncApplicationTests.java` — application test class; reportedly disabled due to external database dependency.

Likely project paths:

```text
backend/
├── pom.xml
├── mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/studysync/
    │   │   ├── StudysyncApplication.java
    │   │   ├── DatabaseController.java
    │   │   ├── controller/StudyMaterialController.java
    │   │   ├── model/StudyMaterial.java
    │   │   └── repository/StudyMaterialRepository.java
    │   └── resources/
    └── test/java/com/studysync/StudysyncApplicationTests.java
```

This is an expected/known structure, not a guarantee that every file is currently present at exactly that path. Inspect the repository before editing.

## 8. Immediate development roadmap

### Milestone 0 — Baseline and safety
 
- [x] Inspect Git status and create a commit/backup before risky changes.
- [x] Inspect the current `pom.xml`, configuration, controllers, entity, repository, and tests.
- [x] Confirm no secrets are committed.
- [x] Run `.\mvnw.cmd clean package` and record the full test summary.
- [x] Confirm the application can start from the current checkout.
 
 **Completion criteria:** A reproducible build, known working baseline, and no uncommitted changes that cannot be recovered.
 
 ### Milestone 1 — Complete CRUD and API reliability
 
- [x] Test create, list, fetch-by-ID, and delete against the database.
- [x] Test missing IDs and invalid request bodies.
- [x] Confirm correct HTTP status codes and response bodies.
- [x] Check validation and exception handling.
- [ ] Add automated mock tests for controller behavior without a live database.
- [ ] Add integration tests for database behavior using an appropriately configured test database or controlled test environment.
- [ ] Avoid disabling tests just to obtain a green build.
 
 **Completion criteria:** CRUD operations behave correctly for success and failure cases, with repeatable test evidence.
 
 ### Milestone 2 — PDF upload and text extraction
 
- [x] Identify the actual upload endpoint, multipart field name, and limits (`POST /api/study-materials/{id}/upload`, field `file`, 5 MB limit).
- [x] Test a valid text-based PDF.
- [x] Confirm extracted text is non-empty and usable.
- [x] Test empty, malformed, corrupt, and unsupported uploads.
- [x] Validate file type and size; do not rely only on a client-supplied filename or MIME type.
- [x] Handle PDFs with no extractable text; OCR can be considered later if required.
- [x] Ensure resources are closed and exceptions are handled.
- [x] Confirm the intended persistence behavior: store extracted text in database (`extracted_text` column in `study_material` table).
- [ ] If files are stored externally, document the storage location and cleanup policy.
 
 **Completion criteria:** A real PDF can be uploaded and processed, and both success and invalid-input paths are tested.

### Milestone 3 — AI-generated summaries

- [x] Choose and configure an LLM provider using an environment variable for the API key (`LLM_API_KEY`, optional `LLM_BASE_URL`, `LLM_MODEL`, `LLM_MAX_TOKENS`, `LLM_MAX_INPUT_CHARS`).
- [x] Create a focused service layer for summary generation (`StudySummaryService`, `LlmClient`, `OpenAiCompatibleLlmClient`).
- [x] Send extracted document text with clear instructions and length limits (validated with max char checks, 413 for oversized input).
- [x] Handle provider timeouts, rate limits, invalid responses, and API errors (proper mapping to 502 Bad Gateway / 503 Service Unavailable).
- [x] Store or return summaries according to the agreed product behavior (persisted to `summary` column on `StudyMaterial` and returned via `SummaryResponse`).
- [x] Test with short and long study materials (8 unit tests with Mockito covering valid execution, missing materials, empty extracted text, oversized input, and provider errors).
- [x] Avoid logging full private document content or secrets.

**Completion criteria:** A user can request a summary of an uploaded document and receive a useful result with predictable error handling.

### Milestone 4 — MCQ and mock-test generation

- [x] Generate MCQs from the selected study material (`QuizService`, `LlmClient.generateMcqs`, `OpenAiCompatibleLlmClient`).
- [x] Define a structured output format: question, options, correct answer, explanation, and source context where possible (4 options, exactly one correct, strict JSON format).
- [x] Validate model output before returning it (verified 4 options, valid matching correct answer, valid JSON array, error mapping to 502).
- [x] Implement answer submission and score calculation server-side (`POST /api/quizzes/{quizId}/submit` returns score, percentage, and detailed feedback per question).
- [x] Test malformed model output and incomplete questions (unit tests for malformed JSON, invalid option counts, missing materials, empty extracted text).
- [x] Add quiz-attempt persistence only after the data model is agreed (`QuizRepository` in-memory session store).

**Completion criteria:** A generated quiz can be answered and scored reliably.

### Milestone 5 — Document-grounded question answering / RAG

- [x] Define text chunking rules and overlap (`TextChunker` with ~1000 char window, ~200 char overlap, sentence/whitespace boundary breaking).
- [x] Generate embeddings for document chunks (`EmbeddingClient`, `OpenAiCompatibleEmbeddingClient` supporting configurable API key, base URL, and model).
- [x] Select and document the vector storage/search approach (dedicated `StudyMaterialChunk` table with serialized embedding vectors, scoped cosine ranking in Java).
- [x] Retrieve relevant chunks for a user's question (`RagService.askQuestion` ranked by cosine similarity with configurable top-k).
- [x] Prompt the model to answer from retrieved evidence and say when the evidence is insufficient (strict anti-hallucination prompt).
- [x] Return source references or snippets when practical (`sources` list with chunkIndex, snippet, and similarity score).
- [x] Test factual grounding, retrieval quality, irrelevant questions, empty input, and provider errors (13 new unit tests in `RagServiceTest` and `TextChunkerTest`).
- [x] Add indexing status, retry handling, and cleanup for deleted documents (`ensureChunksIndexed` on demand).

**Completion criteria:** Answers are based on retrieved document content, with appropriate behavior when the material does not contain the answer.

### Milestone 6 — Authentication, ownership, and data protection

- [x] Decide the authentication approach before exposing user-specific data (Stateless Bearer JWT authentication via Spring Security 7 & JJWT).
- [x] Ensure each user's materials, summaries, and quiz history are isolated (`User` entity, `user_id` foreign key with index on `study_material`, `quiz_attempt` table, user ownership on quizzes).
- [x] Apply authorization checks on every relevant endpoint (server-side verification on all CRUD, PDF upload, summary, quiz generation/fetch/submit, and RAG Q&A endpoints returning 403 Forbidden for cross-user attempts; 401 Unauthorized for missing/invalid tokens).
- [x] Validate uploads and apply size/rate limits (5 MB limit, PDF type check, extracted text size guard).
- [x] Keep secrets out of source control and logs (`JWT_SECRET`, `DB_PASSWORD`, `LLM_API_KEY` externalized in environment variables; BCrypt hashing for passwords).
- [x] Define retention and deletion behavior for uploaded files and extracted text (`deleteById` cascades to associated chunks and vectors).
- [x] Introduce explicit migration strategy for existing records (`DataMigrationRunner` assigns orphaned `study_material` rows to a designated legacy system user).

**Completion criteria:** Users cannot access another user's study materials, and sensitive data is handled deliberately.

### Milestone 7 — Frontend integration and deployment

- [x] Document stable API request/response contracts.
- [x] Connect the frontend upload and material-library screens (React + Vite SPA with drag-and-drop PDF upload, 5 MB file validation, and library cards).
- [x] Add summary, quiz, and Q&A views incrementally (`StudyHub.jsx` with `SummaryTab.jsx`, `QuizTab.jsx` with instant scoring/explanations, and `RagChatTab.jsx` with retrieved source snippets).
- [x] Configure CORS safely for the intended frontend origins (`http://localhost:5173` and `http://127.0.0.1:5173`).
- [ ] Configure deployment environment variables securely.
- [ ] Add health checks, useful logs, and production error handling.
- [ ] Test the deployed application, not only the local environment.

**Completion criteria:** The deployed frontend can use the verified backend workflows end to end.

## 9. Rules for AI coding agents

Any AI agent (Antigravity, Gemini, or another tool) must follow these rules before making changes:

1. Read this document and inspect the actual current code.
2. Treat the code and command output as authoritative; this document may be stale.
3. Identify the root cause before editing. Do not guess at dependencies, APIs, or configuration.
4. Make the smallest changes that solve the requested task.
5. Preserve working endpoints, database configuration, existing behavior, and unrelated files.
6. Do not replace entire files unless genuinely necessary.
7. Never remove dependencies or disable tests simply to make the build pass.
8. Never hard-code or print passwords, API keys, access tokens, or other secrets.
9. Run the most relevant tests and then `.\mvnw.cmd clean package` where appropriate.
10. Read the final build/test output. Distinguish tests that passed from tests that were skipped or never run.
11. For API changes, test actual requests and responses where feasible.
12. If a command fails, diagnose from the first meaningful error and fix only confirmed causes.
13. Do not claim that an endpoint, feature, or deployment works unless it has been tested.
14. At task completion, report:
    - root cause;
    - files changed and why;
    - commands/tests run and their actual results;
    - remaining issues and risks;
    - any documentation updates needed.
15. Update this document whenever a milestone is completed or project behavior changes.

### Recommended agent prompt template

> Read `PROJECT_DOCUMENTATION.md` first. Inspect the current repository and identify the smallest safe implementation for this task: **[describe one task]**. Preserve all verified behavior and secrets. Apply changes directly, run relevant tests and the Maven build, and test affected endpoints where possible. Update the documentation only to reflect verified changes. Report files changed, test/build evidence, and remaining issues. Do not claim success without evidence.

## 10. Definition of done

A task is complete only when all applicable conditions are met:

- [ ] Implementation is present in the intended files.
- [ ] The project compiles.
- [ ] Relevant tests pass, and skipped tests are clearly disclosed.
- [ ] The affected API or workflow has been exercised.
- [ ] Existing verified functionality remains intact.
- [ ] No secrets were added to source control or logs.
- [ ] Error cases have been considered.
- [ ] This document reflects any confirmed change in project status.

## 11. Change log

| Date | Change |
|---|---|
| 14 Oct 2026 | Reported fixes for PDFBox 3 API usage, missing test dependency, and test context startup without database environment variables. `clean package` reported successful, with one test skipped. |
| 18 Oct 2026 | Verified `/api/db-test` alias (200 OK). Added and verified `DELETE /api/study-materials/{id}` (204 No Content, 404 for missing). Verified PDF upload-to-text-extraction (`POST /api/study-materials/{id}/upload`) and persistence in PostgreSQL, as well as rejection of non-PDFs, empty PDFs, and oversized files. |
| 18 Oct 2026 | Implemented Milestone 3 (AI-generated summaries): added `StudySummaryService`, `LlmClient`, `OpenAiCompatibleLlmClient`, `SummaryResponse`, endpoints `POST` and `GET` on `/api/study-materials/{id}/summary`, and database persistence in `study_material.summary`. Added comprehensive unit tests (`StudySummaryServiceTest`) with mocks. `clean package` succeeded with 8 tests passed and 1 integration test skipped. |
| 18 Oct 2026 | Fixed LLM grounding: updated system and user prompts to strictly constrain the model to the source text and disallow hallucinations/inferences when text is brief. Added unit test verifying exact extracted text pass-through to LLM client. Verified live output against material 3. `clean package` succeeded (9 passed, 1 skipped). |
| 18 Oct 2026 | Implemented Milestone 4 (MCQ and mock-test generation): added `QuizQuestion`, `Quiz`, `QuizRepository`, `QuizService`, `QuizController`, and DTOs (`GenerateQuizRequest`, `QuizTestResponse`, `SubmitQuizRequest`, `QuizResultResponse`). Supported counts {5, 10, 20} and difficulties {Easy, Medium, Hard}. Implemented server-side score calculation and answer validation. Added 9 unit tests in `QuizServiceTest`. `clean package` succeeded (19 passed, 1 skipped). |
| 21 Oct 2026 | Implemented Milestone 5 (Document-grounded question answering / RAG): added `StudyMaterialChunk` entity and repository, `TextChunker` (~1000 char window, ~200 char overlap with sentence boundaries), `EmbeddingClient` interface and `OpenAiCompatibleEmbeddingClient`, `RagService` for cosine similarity ranking and source attribution, added `answerQuestion` to `LlmClient` and `OpenAiCompatibleLlmClient` with strict anti-hallucination grounding, and added `POST /api/study-materials/{id}/ask` in `StudyMaterialController`. Added 13 new unit tests across `RagServiceTest` and `TextChunkerTest`. `clean package` succeeded (32 passed, 1 skipped). |
| 21 Oct 2026 | Implemented Milestone 6 (Authentication and user-level data isolation): added `User` (`app_user`) and `QuizAttempt` (`quiz_attempt`) entities with indexed foreign keys, `BCryptPasswordEncoder` (strength 10), stateless JWT auth (`JwtTokenProvider`, `JwtAuthenticationFilter`, `SecurityConfig`, `JwtAuthenticationEntryPoint`, `CustomAccessDeniedHandler`), `AuthService`, and `AuthController` (`POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`). Enforced strict server-side ownership checks on all endpoints (listing only owner's materials, 403 Forbidden on cross-user material reads, updates, deletes, uploads, summaries, RAG Q&A, and quiz operations). Added `DataMigrationRunner` for existing orphaned records. Added automated tests for authentication, unauthorized access, cross-user isolation, and quiz attempts (38 new unit/controller tests). `clean package` succeeded (70 passed, 1 skipped). |
| 22 Oct 2026 | Implemented Milestone 7 — Step 1 (Safe CORS configuration): added `CorsConfigurationSource` bean to `SecurityConfig` allowing strictly `http://localhost:5173` and `http://127.0.0.1:5173` with credentials, allowed HTTP methods (`GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`), allowed headers (`Authorization`, `Content-Type`, `Accept`, `X-Requested-With`, `Origin`), exposed header (`Authorization`), and 1-hour preflight cache. Connected to `SecurityFilterChain` via `.cors(cors -> cors.configurationSource(corsConfigurationSource()))`. Added 6 unit tests in `CorsSecurityTest`. `mvnw.cmd test` succeeded (76 passed, 1 skipped). |
| 23 Oct 2026 | Implemented Milestone 7 — Step 2 (Frontend Integration): built React + Vite application with `lucide-react`, clean CSS design system (`index.css`), light/dark theme toggle (`ThemeContext.jsx`), centralized API client (`api.js`), authentication context (`AuthContext.jsx`), registration and login cards (`AuthCard.jsx`), PDF dropzone upload with file validation (`MaterialUpload.jsx`), library listing (`MaterialList.jsx`), and interactive study workspace (`StudyHub.jsx`) with AI summaries (`SummaryTab.jsx`), MCQ quiz generation & scoring (`QuizTab.jsx`), and document Q&A / RAG (`RagChatTab.jsx`). Frontend production build verified (`npm run build`). |

---

## 12. Next task

**Do next:** Milestone 7 — Step 3: End-to-end browser verification of frontend running against Spring Boot backend.

**Do not start yet:** Production deployment without frontend API integration and CORS origin restrictions.

**Keep this file current:** Update statuses and the change log only after inspecting the code or seeing actual test/endpoint results.

