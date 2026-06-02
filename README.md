# Quizzar API

> **AI-powered quiz generation and management platform for educators.**  
> Teachers generate, share, and analyse quizzes; students take them — all through a clean REST API.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [API Reference](#api-reference)
  - [Authentication](#authentication)
  - [Quiz Management](#quiz-management)
  - [AI Quiz Generation](#ai-quiz-generation)
  - [Public Quiz (Student Flow)](#public-quiz-student-flow)
  - [Analytics](#analytics)
- [AI Generation Pipeline](#ai-generation-pipeline)
- [Security](#security)
- [Caching Strategy](#caching-strategy)
- [Rate Limiting](#rate-limiting)
- [Configuration](#configuration)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Local Development](#local-development)
  - [Running with Docker](#running-with-docker)
- [Environment Variables](#environment-variables)
- [Testing](#testing)
- [Observability](#observability)

---

## Overview

Quizzar API is a Spring Boot backend that lets teachers create quizzes in seconds using Google Gemini AI. A teacher can upload a document (PDF, Word, plain text), paste raw text, or describe a topic with specs — and the AI turns it into a structured, scoreable quiz. Students access the quiz via a shareable link and receive instant feedback, including AI-powered grading for short-answer questions.

---

## Key Features

| Feature | Description |
|---|---|
| **AI Quiz Generation** | Generate quizzes from uploaded files, pasted text, or topic specs using Google Gemini |
| **Three Question Types** | MCQ (4 options), True/False, and Short Answer |
| **AI Short-Answer Grading** | Gemini semantically grades open-ended answers at submission time |
| **AI Timing Suggestions** | AI recommends per-question or overall timing modes for each quiz |
| **Flexible Quiz Modes** | `OVERALL` timer (whole quiz) or `PER_QUESTION` timer |
| **Shareable Quiz Links** | Each quiz gets a unique 8-character code; teachers can regenerate it |
| **Public Student Flow** | No login required for students — just a quiz code |
| **Analytics Dashboard** | Per-quiz and summary analytics for teachers |
| **Redis Caching** | Quiz data, analytics, and public quiz views cached in Redis |
| **Distributed Rate Limiting** | Bucket4j + Redisson enforces per-user/IP rate limits |
| **File Storage** | Uploaded documents stored in AWS S3 with AES-256 server-side encryption |
| **Document Extraction** | Extracts text from PDF, Word (`.docx`/`.doc`), and plain-text files |
| **Keycloak Auth** | JWT-based OAuth2 resource server; teacher accounts auto-provisioned on first login |
| **Flyway Migrations** | Versioned, repeatable database migrations |
| **OpenAPI / Swagger UI** | Interactive API docs at `/swagger-ui.html` |
| **Prometheus Metrics** | Micrometer + Prometheus for metrics scraping |
| **Structured JSON Logging** | JSON log output with trace IDs |

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.5 |
| **Security** | Spring Security + OAuth2 Resource Server (Keycloak) |
| **Database** | PostgreSQL 16 |
| **ORM** | Spring Data JPA / Hibernate |
| **Migrations** | Flyway |
| **Cache** | Redis (Spring Cache + Redisson) |
| **Rate Limiting** | Bucket4j 8 + Redisson (distributed token bucket) |
| **AI** | Google Gemini API (via Spring WebFlux `WebClient`) |
| **File Storage** | AWS S3 (SDK v2) |
| **Document Parsing** | Apache PDFBox (PDF), Apache POI (Word), built-in (text) |
| **API Docs** | SpringDoc OpenAPI 2 / Swagger UI |
| **Metrics** | Micrometer + Prometheus |
| **Retry** | Spring Retry with exponential backoff |
| **Containerisation** | Docker + Docker Compose |
| **Build Tool** | Maven 3.9 |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Quizzar API                          │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────┐  │
│  │   Auth   │  │   Quiz   │  │Generation│  │  Session  │  │
│  │ /api/v1/ │  │ /api/v1/ │  │ /api/v1/ │  │ /public/  │  │
│  │   auth   │  │  quizzes │  │ generate │  │   quiz    │  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └─────┬─────┘  │
│       │              │              │               │        │
│  ┌────▼──────────────▼──────────────▼───────────────▼────┐  │
│  │                  Service Layer                         │  │
│  │  TeacherProvisioning | QuizService | GenerationOrch    │  │
│  │  QuizSessionService  | AnalyticsService                │  │
│  └────────────────────────────┬───────────────────────────┘  │
│                               │                              │
│       ┌───────────────────────┼──────────────────────┐       │
│       │                       │                      │       │
│  ┌────▼─────┐          ┌──────▼────┐         ┌───────▼────┐  │
│  │PostgreSQL│          │  Redis    │         │Gemini API  │  │
│  │(via JPA) │          │  Cache +  │         │(WebClient) │  │
│  └──────────┘          │Rate Limit │         └────────────┘  │
│                        └───────────┘                         │
│  ┌──────────────────────────────────────────────────────┐    │
│  │                    AWS S3                            │    │
│  │  (Teacher document storage with presigned URLs)      │    │
│  └──────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## Project Structure

```
src/main/java/com/quizzar/
├── QuizzarApiApplication.java      # Application entry point
│
├── auth/                           # Authentication & teacher provisioning
│   ├── controller/AuthController   # POST /api/v1/auth/me
│   ├── service/TeacherProvisioningService
│   ├── config/SecurityConfig
│   └── util/SecurityUtils
│
├── quiz/                           # Quiz CRUD
│   ├── controller/QuizController   # GET/PATCH/DELETE /api/v1/quizzes
│   ├── service/QuizService
│   ├── entity/                     # Quiz, QuizMode, TimingMode
│   ├── dto/                        # QuizResponse, QuizSummaryResponse, etc.
│   └── repository/QuizRepository
│
├── question/                       # Question & answer entities
│   ├── entity/                     # Question, AnswerOption, ShortAnswerKey, QuestionType
│   └── repository/
│
├── generation/                     # AI quiz generation
│   ├── controller/GenerationController  # POST /api/v1/generate/*
│   ├── service/
│   │   ├── GenerationOrchestrationService  # Orchestrates all 3 generation modes
│   │   ├── AiGenerationService             # Gemini calls + response parsing
│   │   └── DocumentExtractionService       # PDF / Word / text extraction
│   ├── client/GeminiClient         # Spring WebClient wrapper for Gemini API
│   ├── prompt/                     # ExtractionPromptBuilder, FormattingPromptBuilder, SpecsPromptBuilder
│   └── dto/                        # GenerateFromUploadRequest, GenerateFromPasteRequest, GenerateFromSpecsRequest
│
├── session/                        # Student quiz sessions
│   ├── controller/PublicQuizController  # POST/GET /public/quiz/**
│   ├── service/QuizSessionService
│   ├── entity/                     # QuizSession, SessionAnswer
│   └── dto/                        # StartSessionRequest, SubmitAnswersRequest, QuizResultResponse, etc.
│
├── analytics/                      # Teacher analytics
│   ├── controller/AnalyticsController  # GET /api/v1/analytics/**
│   └── service/AnalyticsService
│
├── storage/                        # AWS S3 file management
│   └── service/S3StorageService
│
├── document/                       # Uploaded document tracking
│   └── entity/UploadedDocument
│
├── teacher/                        # Teacher entity
│   └── entity/Teacher
│
├── cache/                          # Redis cache configuration
│   └── config/CacheConfig
│
├── ratelimit/                      # Distributed rate limiting
│   ├── config/RateLimitConfig
│   └── filter/RateLimitFilter
│
├── config/                         # App-wide beans (WebClient, CORS, etc.)
│
└── common/                         # Shared DTOs, exceptions, handlers
    ├── dto/                        # ApiResponse, ErrorResponse, PageResponse
    ├── exception/                  # Domain exceptions
    └── handler/GlobalExceptionHandler
```

---

## Database Schema

The schema is managed by Flyway with 10 versioned migrations (`V1` – `V10`).

```
teachers
  └── quizzes (teacher_id → teachers.id)
       ├── questions (quiz_id → quizzes.id)
       │    ├── answer_options (question_id → questions.id)
       │    └── short_answer_keys (question_id → questions.id)
       ├── quiz_sessions (quiz_id → quizzes.id)
       │    └── session_answers (session_id → quiz_sessions.id)
       └── uploaded_documents (quiz_id → quizzes.id)
```

| Table | Key Columns |
|---|---|
| `teachers` | `id` (UUID), `keycloak_subject`, `email`, `name` |
| `quizzes` | `id`, `teacher_id`, `title`, `quiz_code` (unique 8-char), `timing_mode`, `quiz_mode`, `timer_value_seconds` |
| `questions` | `id`, `quiz_id`, `question_text`, `question_type` (MCQ/TRUE_FALSE/SHORT_ANSWER), `order_index`, `points` |
| `answer_options` | `id`, `question_id`, `option_label`, `option_text`, `is_correct` |
| `short_answer_keys` | `id`, `question_id`, `accepted_answer`, `is_case_sensitive` |
| `quiz_sessions` | `id`, `quiz_id`, `student_name`, `ip_address`, `started_at`, `completed_at`, `total_score`, `max_score` |
| `session_answers` | `id`, `session_id`, `question_id`, `selected_option_id`, `answer_text`, `is_correct`, `time_taken_seconds` |
| `uploaded_documents` | `id`, `quiz_id`, `s3_key`, `original_filename`, `content_type`, `size_bytes` |

---

## API Reference

All authenticated endpoints require a valid Bearer JWT issued by Keycloak.

### Authentication

#### `POST /api/v1/auth/me`
Provisions a teacher account on first login, or returns the existing profile.

**Auth:** Required (Bearer JWT)

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "keycloakSubject": "string",
    "email": "teacher@example.com",
    "name": "Jane Doe"
  }
}
```

---

### Quiz Management

All endpoints under `/api/v1/quizzes` are authenticated. Teachers can only access their own quizzes.

#### `GET /api/v1/quizzes`
Paginated list of the authenticated teacher's quizzes.

| Query Param | Default | Description |
|---|---|---|
| `page` | `0` | Page number (0-indexed) |
| `size` | `10` | Page size |
| `sortBy` | `createdAt` | Sort field |
| `sortDir` | `desc` | `asc` or `desc` |

**Response:** Paginated `QuizSummaryResponse` (id, title, description, quizCode, timingMode, questionCount, createdAt).

---

#### `GET /api/v1/quizzes/{quizId}`
Full quiz detail including all questions and answer options.

---

#### `PATCH /api/v1/quizzes/{quizId}`
Update quiz metadata. All fields are optional (partial update).

**Request body:**
```json
{
  "title": "string",
  "description": "string",
  "quizMode": "OVERALL | PER_QUESTION",
  "timingMode": "NONE | OVERALL | PER_QUESTION",
  "timerValueSeconds": 30
}
```

> **Note:** Setting `quizMode` to `PER_QUESTION` with a positive `timerValueSeconds` automatically sets `timingMode` to `PER_QUESTION`.

---

#### `DELETE /api/v1/quizzes/{quizId}`
Deletes the quiz, all associated questions, sessions, and S3 documents.

---

#### `GET /api/v1/quizzes/{quizId}/link`
Returns the quiz code and the shareable public URL.

**Response:**
```json
{
  "success": true,
  "data": {
    "quizCode": "ABC12345",
    "shareUrl": "https://your-domain.com/public/quiz/ABC12345"
  }
}
```

---

#### `POST /api/v1/quizzes/{quizId}/regenerate-code`
Generates a new unique quiz code and returns the updated share URL.

---

### AI Quiz Generation

All generation endpoints are authenticated and rate-limited to **10 requests/minute** per user.

Supported file types: `application/pdf`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document`, `application/msword`, `text/plain`. Maximum file size: **25 MB**.

---

#### `POST /api/v1/generate/upload`
Generate a quiz by uploading a document (PDF, Word, or text).

**Content-Type:** `multipart/form-data`

| Part | Type | Required | Description |
|---|---|---|---|
| `request` | JSON | Yes | Generation options (see below) |
| `file` | File | Yes | The document to extract text from |

**`request` JSON:**
```json
{
  "quizTitle": "string",
  "quizDescription": "string",
  "quizMode": "OVERALL | PER_QUESTION",
  "timingPreference": "AI_SUGGESTED | NONE | OVERALL | PER_QUESTION",
  "manualTimerSeconds": 30
}
```

---

#### `POST /api/v1/generate/paste`
Generate a quiz from raw pasted text.

**Content-Type:** `application/json`

```json
{
  "quizTitle": "string",
  "quizDescription": "string",
  "rawText": "Your full study notes or content here...",
  "quizMode": "OVERALL | PER_QUESTION",
  "timingPreference": "AI_SUGGESTED | NONE | OVERALL | PER_QUESTION",
  "manualTimerSeconds": 30
}
```

---

#### `POST /api/v1/generate/specs`
Generate a quiz from topic specifications, with an optional syllabus file for context.

**Content-Type:** `multipart/form-data`

| Part | Type | Required | Description |
|---|---|---|---|
| `request` | JSON | Yes | Topic specs |
| `syllabusFile` | File | No | Optional syllabus document for context |

**`request` JSON:**
```json
{
  "quizTitle": "string",
  "quizDescription": "string",
  "topic": "Photosynthesis",
  "subtopics": ["Light reactions", "Calvin cycle"],
  "difficulty": "MEDIUM",
  "numberOfQuestions": 10,
  "questionTypes": ["MCQ", "TRUE_FALSE", "SHORT_ANSWER"],
  "syllabusText": "Optional plain-text syllabus if no file is uploaded",
  "quizMode": "OVERALL | PER_QUESTION",
  "timingPreference": "AI_SUGGESTED | NONE | OVERALL | PER_QUESTION",
  "manualTimerSeconds": 30
}
```

**Generation Response (all three endpoints):**
```json
{
  "success": true,
  "data": {
    "quizId": "uuid",
    "quizCode": "ABC12345",
    "shareUrl": "https://your-domain.com/public/quiz/ABC12345",
    "aiTimingSuggestion": {
      "timingMode": "PER_QUESTION",
      "timeSeconds": 30,
      "reasoning": "Questions require moderate reading time..."
    }
  }
}
```

---

### Public Quiz (Student Flow)

No authentication required. Rate limits apply by IP address.

#### `GET /public/quiz/{quizCode}`
Fetch the public quiz view (questions without correct answers).

---

#### `POST /public/quiz/{quizCode}/start`
Start a new quiz session.

**Request:**
```json
{ "studentName": "Alice" }
```

**Response:**
```json
{
  "success": true,
  "data": {
    "sessionId": "uuid",
    "timingMode": "PER_QUESTION | OVERALL | NONE",
    "timerValueSeconds": 30
  }
}
```

---

#### `POST /public/quiz/{quizCode}/sessions/{sessionId}/submit`
Submit all answers at once (batch mode).

**Request:**
```json
{
  "answers": [
    {
      "questionId": "uuid",
      "selectedOptionId": "uuid",
      "answerText": "string (for SHORT_ANSWER)",
      "timeTakenSeconds": 12
    }
  ]
}
```

---

#### `POST /public/quiz/{quizCode}/sessions/{sessionId}/submit-answer`
Submit a single answer immediately (per-question mode). Returns instant feedback.

**Request:**
```json
{
  "questionId": "uuid",
  "selectedOptionId": "uuid",
  "answerText": "string",
  "timeTakenSeconds": 10
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "isCorrect": true,
    "pointsEarned": 1,
    "correctOptionLabel": "B",
    "correctOptionText": "Mitochondria",
    "correctShortAnswerKeys": []
  }
}
```

---

#### `POST /public/quiz/{quizCode}/sessions/{sessionId}/complete`
Finalise a per-question-mode session and compute the final score.

---

#### `GET /public/quiz/{quizCode}/sessions/{sessionId}/results`
Retrieve full session results (score, per-question breakdown).

**Response:**
```json
{
  "success": true,
  "data": {
    "sessionId": "uuid",
    "quizId": "uuid",
    "studentName": "Alice",
    "totalScore": 8,
    "maxScore": 10,
    "percentageScore": 80.0,
    "passed": true,
    "completedAt": "2024-01-15T10:30:00Z",
    "details": [
      {
        "questionId": "uuid",
        "questionText": "What is the powerhouse of the cell?",
        "questionType": "MCQ",
        "selectedOptionLabel": "B",
        "selectedOptionText": "Mitochondria",
        "correctOptionLabel": "B",
        "correctOptionText": "Mitochondria",
        "isCorrect": true,
        "pointsEarned": 1,
        "maxPoints": 1
      }
    ]
  }
}
```

---

### Analytics

All endpoints authenticated. Teachers see only their own data.

#### `GET /api/v1/analytics/summary`
High-level statistics: total quizzes, sessions, pass rate, etc.

#### `GET /api/v1/analytics/quizzes/{quizId}`
Detailed analytics for a specific quiz: total attempts, average score, question-level accuracy.

#### `GET /api/v1/analytics/sessions/{sessionId}/results`
Full result detail for any session belonging to the teacher's quiz.

---

## AI Generation Pipeline

```
Teacher Request
      │
      ▼
GenerationOrchestrationService
      │
      ├─[upload] → DocumentExtractionService (PDFBox/POI) → extracted text
      │                                                           │
      ├─[paste]  → raw text ─────────────────────────────────────┤
      │                                                           │
      └─[specs]  → SpecsPromptBuilder ──────────────────────────►│
                                                                  │
                              PromptBuilder (Extraction/Formatting/Specs)
                                                                  │
                                                                  ▼
                                                      AiGenerationService
                                                 (@Retryable, up to 3 attempts,
                                                  exponential backoff 2s→10s)
                                                                  │
                                                                  ▼
                                                         GeminiClient
                                                    (WebClient → Gemini API,
                                                     JSON response mode)
                                                                  │
                                                                  ▼
                                                   Parse & validate AiQuizGenerationResult
                                                   (questions, timing suggestion)
                                                                  │
                                                                  ▼
                                                   Persist Quiz + Questions
                                                   (with AI timing applied)
```

### Short Answer Grading

For **per-question mode**, Gemini is called synchronously on each short-answer submission. A local case-insensitive exact match is tried first; Gemini is only called on a mismatch.

For **batch submission mode**, all short-answer questions that fail local matching are sent to Gemini in a single batch call with retry.

Grading failures default to `false` (conservative) to prevent crashes.

---

## Security

- **OAuth2 JWT Resource Server:** All `/api/v1/**` endpoints require a valid JWT issued by Keycloak.
- **Teacher Provisioning:** On the first authenticated call to `POST /api/v1/auth/me`, a `Teacher` record is automatically created from the JWT claims (`sub`, `email`, `name`).
- **Ownership Enforcement:** Every quiz/session operation verifies the Keycloak subject matches the resource owner before proceeding.
- **Public Endpoints:** `/public/quiz/**` are intentionally unauthenticated but are protected by IP-based rate limiting.
- **S3 Encryption:** All uploaded files are stored with AES-256 server-side encryption.

---

## Caching Strategy

Redis caching is applied via Spring `@Cacheable` / `@CacheEvict` annotations with a default TTL of **10 minutes**.

| Cache Name | Key | Evicted On |
|---|---|---|
| `quiz-list` | `{keycloakSubject}-{page}-{size}-{sort}` | Quiz create, update, delete |
| `quiz-detail` | `{quizId}` | Quiz update, delete |
| `public-quiz` | `{quizCode}` | Quiz update, delete, code regeneration |
| `analytics` | `{quizId}` or `summary-{subject}` | Session submission, quiz delete |

---

## Rate Limiting

Rate limiting is implemented with **Bucket4j** backed by **Redisson** (Redis), ensuring limits are enforced consistently across multiple application instances.

| Endpoint Pattern | Limit |
|---|---|
| `POST /api/v1/generate/**` | 10 requests / minute / user |
| `POST /public/quiz/**/submit` | 30 requests / minute / IP |
| `GET /public/quiz/**` | 60 requests / minute / IP |
| All other endpoints | 120 requests / minute / user |

When a limit is exceeded, the API responds with HTTP `429 Too Many Requests` and a `Retry-After` header indicating when to retry.

---

## Configuration

Key `application.properties` settings (provided via environment variables):

| Property | Env Variable | Description |
|---|---|---|
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | DB username |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | DB password |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | Keycloak realm issuer URI |
| `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` | `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` | Keycloak JWK set URI |
| `quizzar.gemini.api-key` | `GEMINI_API_KEY` | Google Gemini API key |
| `quizzar.gemini.model` | `GEMINI_MODEL` | Gemini model name (e.g. `gemini-2.0-flash`) |
| `spring.data.redis.url` | `SPRING_DATA_REDIS_URL` | Redis connection URL |
| `aws.s3.bucket-name` | `AWS_S3_BUCKET_NAME` | S3 bucket name |
| `aws.s3.region` | `AWS_S3_REGION` | AWS region |
| `aws.s3.presigned-url-expiry-minutes` | `AWS_S3_PRESIGNED_URL_EXPIRY_MINUTES` | Presigned URL TTL |
| `quizzar.base-url` | `QUIZZAR_BASE_URL` | Public base URL (used in share links) |
| `quizzar.max-file-size-mb` | `QUIZZAR_MAX_FILE_SIZE_MB` | Max upload size in MB (default: 20) |
| `quizzar.pass-threshold-percent` | `QUIZZAR_PASS_THRESHOLD_PERCENT` | Pass mark percentage (default: 50) |
| `fe.base-url` | `FE_URL` | Frontend URL (for CORS) |

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### Local Development

1. **Clone the repository:**
   ```bash
   git clone https://github.com/PreciousIfeaka/quizzar-api.git
   cd quizzar-api
   ```

2. **Start infrastructure services:**
   ```bash
   docker-compose up -d postgres redis keycloak
   ```
   This starts PostgreSQL on `5432`, Redis on `6379`, and Keycloak on `8180`.

3. **Configure Keycloak:**
   - Open `http://localhost:8180` and log in with `admin` / `admin`.
   - Create a realm and a client for the Quizzar API.
   - Note the issuer URI (e.g., `http://localhost:8180/realms/quizzar`).

4. **Create `src/main/resources/application-local.properties`** with your local overrides:
   ```properties
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/quizzar_db
   SPRING_DATASOURCE_USERNAME=quizzar
   SPRING_DATASOURCE_PASSWORD=quizzar
   SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8180/realms/quizzar
   SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://localhost:8180/realms/quizzar/protocol/openid-connect/certs
   GEMINI_API_KEY=your-gemini-api-key
   GEMINI_MODEL=gemini-2.0-flash
   SPRING_DATA_REDIS_URL=redis://localhost:6379
   AWS_S3_BUCKET_NAME=quizzar-dev
   AWS_S3_REGION=us-east-1
   AWS_S3_PRESIGNED_URL_EXPIRY_MINUTES=60
   QUIZZAR_BASE_URL=http://localhost:8080
   FE_URL=http://localhost:3000
   ```

5. **Run the application:**
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

6. **Access the API:**
   - Base URL: `http://localhost:8080`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - OpenAPI JSON: `http://localhost:8080/api-docs`

### Running with Docker

Use the full Docker Compose stack (includes the app image from GHCR):

```bash
# Create a .env file with all required variables (see Environment Variables section)
docker-compose up -d
```

The app will be available on port `8081` (mapped to container port `8080`).

---

## Environment Variables

Create a `.env` file in the project root for Docker Compose. Reference the [Configuration](#configuration) table above for all required variables. The `.env` file is git-ignored.

---

## Testing

The project uses:
- **JUnit 5** with **Mockito** for unit tests
- **Testcontainers** (PostgreSQL + Redis) for integration tests
- **OkHttp MockWebServer** for Gemini client tests
- **Spring Security Test** for authentication context

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=QuizServiceTest

# Run integration tests only
./mvnw test -Dtest=*IntegrationTest
```

---

## Observability

### Health & Metrics

The following Actuator endpoints are exposed:

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | Application health (DB, Redis connectivity) |
| `GET /actuator/info` | Application info |
| `GET /actuator/metrics` | All Micrometer metrics |
| `GET /actuator/prometheus` | Prometheus scrape endpoint |

### Logging

Logs are emitted in JSON format with the following structure:

```json
{
  "timestamp": "2024-01-15T10:30:00.000Z",
  "level": "INFO",
  "traceId": "abc123",
  "logger": "com.quizzar.quiz.service.QuizService",
  "message": "Deleted quiz abc-uuid and 2 S3 documents"
}
```

---

## Error Responses

All errors follow a consistent structure:

```json
{
  "success": false,
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 404,
  "error": "QUIZ_NOT_FOUND",
  "message": "Quiz not found: abc-uuid",
  "path": "/api/v1/quizzes/abc-uuid",
  "traceId": "abc123"
}
```

| Error Code | HTTP Status | Cause |
|---|---|---|
| `QUIZ_NOT_FOUND` | 404 | Quiz does not exist |
| `FORBIDDEN` | 403 | Teacher does not own the resource |
| `VALIDATION_ERROR` | 400 | Request body failed validation |
| `INVALID_FILE_TYPE` | 400 | Unsupported file format |
| `FILE_TOO_LARGE` | 400 | File exceeds size limit |
| `EXTRACTION_FAILED` | 422 | Could not extract text from file |
| `AI_SERVICE_ERROR` | 502 | Gemini API error after retries |
| `STORAGE_ERROR` | 502 | AWS S3 operation failed |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## License

This project is proprietary. All rights reserved.
