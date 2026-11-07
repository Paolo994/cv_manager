# CV Manager

A multi-agent web application that helps job seekers manage their CVs, get structured feedback on them,
and prepare tailored applications. Specialised AI agents built on Claude collaborate: one reads the CV and
builds a structured profile, others analyse a job posting, tailor the CV, write the cover letter, and a
Critic agent checks every claim against the candidate's real profile so that nothing is invented.

Spring Boot 4.1 backend (BFF with Spring Security) and a React SPA, shipped together as a single JAR.

## Features

- **Accounts**: registration and login with a server-side session (HttpOnly cookie, CSRF protection);
  every user only ever sees their own data.
- **CV upload**: PDF and DOCX, text extracted with Apache Tika; the file type is detected from its content,
  and scanned PDFs without selectable text are rejected with a clear message.
- **Candidate profile**: extracted by the CV Parser agent, then reviewed, edited and confirmed by the user.
  Years of experience are computed in code from the dates, not estimated by the model.
- **CV review**: per-criterion scores (clarity, impact, structure, ATS compatibility and, optionally, fit
  with a job posting), strengths, and prioritised suggestions with rewrite examples; history per CV.
- **Job postings**: paste any posting (LinkedIn included): the Job Analyzer extracts requirements,
  keywords, language, work mode and salary.
- **Tailored applications**: cover letter and tailored CV written in parallel, verified by the Critic and
  sent back for revision when needed (at most twice). Remaining findings stay visible to the user.
- **Export**: CV and cover letter as PDF or DOCX, with contact details in the header.
- **Job preferences**: target roles, locations, work mode, minimum salary, seniority, excluded industries.
- **Live progress**: every agent step is streamed to the browser with Server-Sent Events.

Job search across job boards is planned for the next phase (see [Roadmap](#roadmap)).

## How the agents collaborate

The orchestrator is plain Java code, not an LLM: workflows are explicit, predictable and testable.
Agents exchange typed Java records; Claude returns structured output validated against the record's JSON
schema. Every run and every agent step (model, tokens, output) is stored in the database.

| Agent               | Input                                   | Output                                           | Default model |
|---------------------|-----------------------------------------|--------------------------------------------------|---------------|
| CV Parser           | Extracted CV text                       | Structured candidate profile                     | Sonnet 5.5    |
| CV Reviewer         | CV text, optional job posting           | Scores, strengths, suggestions                   | Sonnet 5.5    |
| Job Analyzer        | Job posting text                        | Requirements, keywords, language, work mode      | Haiku 5.5     |
| CV Tailor           | Confirmed profile, job posting          | Tailored CV and an explanation of the changes    | Sonnet 5.5    |
| Cover Letter Writer | Confirmed profile, job posting, tone    | Cover letter                                     | Opus 5.5      |
| Critic              | Confirmed profile, generated documents  | Unsupported or inaccurate claims, style findings | Sonnet 5.5    |

Application workflow: `Job Analyzer` (if needed) → `CV Tailor` ∥ `Cover Letter Writer` → `Critic`
→ targeted revision of the document with issues → `Critic` again (at most two revisions).

The user stays in control at three checkpoints: confirming the profile, choosing the job posting,
and sending the application (the app never applies on the user's behalf).

Models and token limits are configured per agent in `application.yml` (`app.agents.*`), and prompts live
in `backend/src/main/resources/prompts/` so they can be changed without touching the code.

## Privacy and security

- Contact details (email, phone, LinkedIn) are stored separately and used only when exporting documents:
  they are never sent to the model, and a test enforces it.
- The CV Parser does not extract personal data such as phone numbers, addresses or date of birth.
- External content (CVs and job postings) is passed to the model as delimited data, never as instructions.
- LinkedIn is never scraped: users paste the posting text and apply on LinkedIn themselves.
- Uploaded CVs are stored outside the repository (`data/`, git-ignored); secrets come from environment variables.

## Tech stack

| Area          | Choice                                                        |
|---------------|---------------------------------------------------------------|
| Backend       | Java 21, Spring Boot 4.1, Spring Security 7, Spring Session JDBC |
| AI            | Spring AI 2.0 with the Anthropic starter (Claude)             |
| Persistence   | PostgreSQL 16 + pgvector, Spring Data JPA, Flyway             |
| Documents     | Apache Tika (reading), PDFBox (PDF), Apache POI (DOCX)        |
| Frontend      | React 19, TypeScript, Vite, React Router, TanStack Query      |
| Tests         | JUnit 5, Testcontainers (PostgreSQL), LLM replaced by a mock  |

## Getting started

### Requirements

- Java 21 and Maven 3.9+
- Docker (local PostgreSQL and Testcontainers)
- Node 22, only for frontend development (the Maven build downloads its own Node)
- An [Anthropic API key](https://console.anthropic.com/)

### Configuration

```bash
cp .env.example .env   # then set DB_PASSWORD and ANTHROPIC_API_KEY
```

`.env` is git-ignored. Both `docker compose` and the application read it.

| Variable            | Default                                       | Notes                               |
|---------------------|-----------------------------------------------|-------------------------------------|
| `ANTHROPIC_API_KEY` | none                                          | Required for the agents             |
| `DB_PASSWORD`       | none                                          | Required                            |
| `DB_URL`            | `jdbc:postgresql://localhost:5433/cvmanager`  |                                     |
| `DB_USER`           | `cvmanager`                                   |                                     |
| `STORAGE_DIR`       | `./data/uploads`                              | Where uploaded CVs are stored       |
| `COOKIE_SECURE`     | `false`                                       | Set to `true` in production (HTTPS) |

### Development

```bash
# Backend on :8080 (starts docker-compose.yml automatically -> PostgreSQL on localhost:5433)
mvn -pl backend -am spring-boot:run "-Dskip.npm" "-Dskip.installnodenpm"

# Frontend on :5173 with hot reload (/api is proxied to :8080)
cd frontend
npm install
npm run dev
```

Open http://localhost:5173.

### Build, test and run

```bash
mvn verify                                # builds React and the backend, runs the tests (Docker required)
docker compose up -d                      # PostgreSQL must be running before starting the JAR
java -jar backend/target/cv-manager.jar   # full app on http://localhost:8080
```

Run the JAR from the project root so that it finds `.env`. If startup fails with
`Connection to localhost:5433 refused`, PostgreSQL is not running: start it with `docker compose up -d`.

`-Dskip.npm -Dskip.installnodenpm` skips the frontend build when only the backend changed.
Tests never call the Anthropic API: the LLM gateway is mocked.

## Project structure

```text
cv_manager/
├─ pom.xml                     multi-module parent
├─ docker-compose.yml          PostgreSQL + pgvector
├─ backend/
│  └─ src/main/java/it/cvmanager/
│     ├─ auth/                 registration, login, users
│     ├─ cv/                   CV upload, text extraction, reviews
│     ├─ profile/              candidate profile, contacts, job preferences
│     ├─ jobs/                 job postings and their analysis
│     ├─ documents/            tailored CVs, cover letters, PDF/DOCX export
│     ├─ agents/               agents, workflows and the orchestrator (runs, steps, SSE)
│     └─ shared/               LLM gateway, file storage, errors
└─ frontend/                   React + Vite SPA
```

## API overview

All endpoints except authentication require a session. Every non-GET request must send the
`X-XSRF-TOKEN` header with the value of the `XSRF-TOKEN` cookie (call `GET /api/auth/csrf` first).
Errors are returned as RFC 9457 Problem Details.

| Method | Path                                    | Description                                         |
|--------|-----------------------------------------|-----------------------------------------------------|
| POST   | `/api/auth/register`                    | Create an account (JSON `{email, password, fullName}`) |
| POST   | `/api/auth/login`                       | Log in (form `email`, `password`)                   |
| POST   | `/api/auth/logout`                      | Log out                                             |
| GET    | `/api/auth/me`                          | Current user                                        |
| POST   | `/api/cvs`                              | Upload a CV (multipart `file`)                      |
| GET    | `/api/cvs`, `/api/cvs/{id}`             | List CVs, CV details with extracted text            |
| GET    | `/api/cvs/{id}/reviews`                 | Review history of a CV                              |
| GET    | `/api/profile`, PUT `/api/profile`      | Read, edit and confirm the profile                  |
| GET/PUT| `/api/profile/contact`                  | Contact details (export only)                       |
| GET/PUT| `/api/preferences`                      | Job search preferences                              |
| POST   | `/api/jobs`                             | Add a job posting (pasted text, optional URL)       |
| GET    | `/api/jobs/{id}/documents`              | Generated documents, all versions                   |
| POST   | `/api/documents/{id}/revisions`         | Save an edited cover letter as a new version        |
| GET    | `/api/documents/{id}/export?format=pdf` | Export as `pdf` or `docx`                           |
| POST   | `/api/workflows/{workflow}`             | Start `import-cv`, `review-cv`, `analyze-job` or `prepare-application` (returns `202` with `runId`) |
| GET    | `/api/runs/{id}`                        | Run status with its agent steps                     |
| GET    | `/api/runs/{id}/events`                 | Live run updates (Server-Sent Events)               |

## Roadmap

- [x] **Phase 1**: project foundations, authentication, React SPA
- [x] **Phase 2**: CV upload, CV Parser, editable profile, preferences, CV Reviewer, live agent progress
- [x] **Phase 3**: job postings, Job Analyzer, CV Tailor, Cover Letter Writer, Critic, PDF/DOCX export
- [ ] **Phase 4**: job search via public APIs (Adzuna, Remotive), local embedding model and pgvector
      pre-filter, Job Matcher with scores and reasons, pre-filled LinkedIn search links
- [ ] **Phase 5**: application assistant and application tracker
- [ ] **Phase 6**: usage limits, cost metrics, email verification, Google sign-in, deployment
