# Project Progress Log

## Session — 2026-04-22

### v9 — Nginx reverse proxy + multi-instance deployment

- Renamed `app` → `app1` + `app2` in `compose.yaml`; neither exposes a port directly
- Added `nginx:alpine` service on port 80 with round-robin upstream to `app1:8080` and `app2:8080`
- Created `nginx/nginx.conf` with upstream block and `proxy_set_header` forwarding
- Fixed Liquibase race condition: `app1` has a healthcheck (curl `8081/actuator/health`), `app2` depends on `app1: service_healthy` so migrations only run once
- Added `spring-boot-starter-actuator` and `micrometer-registry-prometheus` to `pom.xml`
- Management server moved to port `8081` (separate from main API on `8080`)
- Exposed endpoints: `health`, `info`, `metrics`, `prometheus`, `loggers`
- `health.show-details: when-authorized` — anonymous callers see only `UP/DOWN`, authenticated users see full component details
- `health.probes.enabled: true` — Kubernetes-style `/liveness` and `/readiness` sub-probes available
- Added `info.app` block (name, version) visible at `/actuator/info`
- Port 8081 never mapped in `compose.yaml` — internal only (used by healthcheck and Prometheus scraper)
- `SecurityConfig` updated to permit `/actuator/health` on the main port as well
- Created `docs/v9-nginx-multi-instance.md` with full architecture notes

---

## Session — 2026-04-20

### What was built

This session covered the full infrastructure layer on top of the existing v1–v2 Kotlin source code.

---

### v3 — Database layer

- **Entity:** `Movie` with fields `id`, `title`, `description`, `year`, `genre`, `director`, `rating` (nullable Double)
- **Entity:** `User` with fields `id`, `username`, `email`, `password`, `role` (enum: USER / ADMIN)
- **Repositories:** `MovieRepository` (with custom `search` JPQL query), `UserRepository`
- **DTOs:** `MovieRequest`, `MovieResponse`, `UserRequest`, `UserResponse`
- **Service:** `MovieService` with full CRUD + search; `UserService`; `AuthService` (JWT login)
- **Controller:** `MovieController` (`/movies`), `UserController` (`/users`), `AuthController` (`/auth/login`)
- **Security:** JWT filter, BCrypt password encoding, role-based access (ADMIN for write, public for GET movies)
- **DataSeeder:** seeds 2 users and 6 movies on startup (dev only — `@Profile("dev")` planned)

---

### Liquibase — schema migrations

| File | Purpose |
|------|---------|
| `db/changelog/db.changelog-master.xml` | Master changelog, includes all changesets |
| `changes/001-create-tables.xml` | Creates `users` and `movies` tables (TEXT columns) |
| `changes/002-seed-users.xml` | Seeds admin + user with BCrypt(10) hashed passwords |
| `changes/003-seed-movies.xml` | Seeds 6 movies |
| `changes/004-add-movie-rating.xml` | Adds `rating DOUBLE PRECISION` column to movies |

- `ddl-auto: none` — Liquibase owns schema, Hibernate only validates
- `relativeToChangelogFile="true"` used in all includes for JAR compatibility
- `spring.liquibase.enabled: true` set explicitly

---

### Swagger / OpenAPI

- Dependency: `springdoc-openapi-starter-webmvc-ui:2.8.8`
- Swagger UI available at: `http://localhost:8080/swagger-ui/index.html`
- Security config updated to permit `/swagger-ui/**` and `/v3/api-docs/**` without auth

---

### Docker

**`compose.yaml`**
- `postgres` service (PostgreSQL 16) with healthcheck
- `app` service — builds from Dockerfile, depends on postgres healthy
- Explicit `backend` bridge network connecting both services

**`Dockerfile`** — multi-stage build (Maven build → JRE runtime)

**`scripts/`**
```
scripts/db.sh   start | stop | fresh | app
scripts/app.sh  build | test | run
```

---

### GitLab CI (`gitlab-ci.yml`)

Stages: `build` → `test` → `migrate` → `docker`

| Stage | What it does |
|-------|-------------|
| `build` | `bash scripts/app.sh build` — compiles JAR |
| `test` | `bash scripts/app.sh test` — runs test suite |
| `migrate` | Runs `liquibase update` against a Postgres service container |
| `docker` | Builds and pushes Docker image to GitLab registry (main branch only) |

CI variables defined: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`

---

### Known issues / next steps

| # | Item | Status |
|---|------|--------|
| 1 | Liquibase not triggering on first run (tables missing) | Under investigation — `relativeToChangelogFile` fix applied, needs retest |
| 2 | `DataSeeder` should be `@Profile("dev")` only | Pending |
| 3 | Split changelogs: schema (all envs) vs seed data (dev only) | Pending |
| 4 | Extract credentials to environment variables / secrets | Pending |
| 5 | Verify BCrypt hashes in `002-seed-users.xml` match actual passwords | Pending |
| 6 | `springdoc 2.8.8` built for Spring Boot 3.x — verify compatibility with 4.0.5 | Pending |
| 7 | Remove redundant `hibernate.dialect` property (Hibernate 7 warns it's unnecessary) | Pending |

---

### How to run locally

```bash
# DB only (app runs in IDE)
bash scripts/db.sh start
bash scripts/app.sh run

# Full Docker
docker compose down -v && docker compose up --build
```
