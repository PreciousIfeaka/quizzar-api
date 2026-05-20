# Quizzar API

AI-Powered Quiz Generation Platform.

## Prerequisites
- Java 21
- Docker & Docker Compose
- Maven

## Getting Started
1. Start infrastructure:
   ```bash
   docker-compose up -d
   ```
2. Configure environment variables (see `application.yml`).
3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

## API Documentation
Once running, visit:
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- API Docs: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## Features
- AI Quiz Generation (Upload, Paste, Specs)
- Public Quiz Links for Students
- Automated Scoring & Semantic Short Answer Grading
- Teacher Analytics Dashboard
- Redis Caching & Rate Limiting
- S3 Document Storage
