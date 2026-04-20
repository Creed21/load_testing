# Spring Boot Load Testing — Subject App

A Spring Boot (Kotlin) REST API built as a subject application for load, stress, and spike testing research. The app is designed so that each version introduces exactly one architectural concern, allowing load test runs to isolate the performance cost of each layer.

## Stack

| Component | Detail |
|-----------|--------|
| Language | Kotlin 2.x |
| Framework | Spring Boot 4.x |
| Build | Maven |
| Database | PostgreSQL 16 |
| Migrations | Liquibase |
| Containerization | Docker + Docker Compose |
| CI/CD | GitLab CI |
| API Docs | Swagger UI (`/swagger-ui/index.html`) |

## Version Roadmap

Each version adds one concept on top of the previous.

| Version | Focus |
|---------|-------|
| v1 | Plain REST — single controller, hardcoded response |
| v2 | Layered architecture — controller / service / repository / entity / DTO |
| v3 | Database — JPA + PostgreSQL, Liquibase migrations, real persistence |
| v4 | Validation & error handling — Bean Validation, global exception handler |
| v5 | Security — JWT authentication, BCrypt, role-based access (USER / ADMIN) |
| v6 | Tests — unit (JUnit 5) and integration (`@SpringBootTest`) |
| v7 | Docker + profiles — Dockerfile, docker-compose, dev/prod profiles |

## Domain

Two main resources:
- **Movie** — `id`, `title`, `description`, `year`, `genre`, `director`, `rating`
- **User** — `id`, `username`, `email`, `password`, `role` (USER / ADMIN)

Key endpoints:

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/movies` | Public | List / search movies |
| `GET` | `/movies/{id}` | Public | Get movie by ID |
| `POST` | `/movies` | ADMIN | Create movie |
| `PUT` | `/movies/{id}` | ADMIN | Update movie |
| `DELETE` | `/movies/{id}` | ADMIN | Delete movie |
| `GET` | `/users` | Public | List users |
| `GET` | `/users/{id}` | Public | Get user by ID |
| `POST` | `/users` | Public | Create user |
| `POST` | `/auth/login` | — | Obtain JWT token |

## Running Locally

### DB only (app runs in IDE)

```bash
bash scripts/db.sh start
bash scripts/app.sh run
```

### Full Docker

```bash
docker compose up --build
```

### Tear down

```bash
docker compose down -v
```

## Testing

```bash
# Unit tests (no Spring context, fast)
mvn test

# Unit + integration tests with JaCoCo coverage report
mvn verify

# Coverage report
open target/site/jacoco/index.html
```

## Load / Stress / Spike Testing

Tests are run manually with **k6** or **Apache JMeter** against the Dockerized app on port `8080`.

| Test type | Tool | VUs | Duration |
|-----------|------|-----|----------|
| Load (baseline) | k6 / JMeter | 50 | 2 min per endpoint |
| Stress (break point) | k6 ramp-up | 0 → 200 | 12 min total |
| Spike | k6 instant jump | 10 → 500 | 30 s spike + 2 min recovery |

Success criteria: p95 < 200 ms, error rate < 1 % under normal load.

## CI/CD (GitLab)

Pipeline stages: `build` → `test` → `migrate` → `docker`

The `docker` stage builds and pushes the image to the GitLab registry on the `main` branch only.

## Project Structure

```
src/
  main/kotlin/fon/master/load/testing/
    controller/       # REST controllers
    service/          # Business logic
    repository/       # Spring Data JPA repositories
    entity/           # JPA entities
    dto/              # Request / response DTOs
    security/         # JWT filter, security config
    config/           # App configuration
    exception/        # Custom exceptions + global handler
  main/resources/
    db/changelog/     # Liquibase migration changesets
    application.yaml
    application-dev.yaml
    application-prod.yaml
scripts/
  db.sh               # start | stop | fresh | app
  app.sh              # build | test | run
docs/                 # Roadmap, testing plan, version guides, progress log
```