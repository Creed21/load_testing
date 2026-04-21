# New Developer Guide

## What is this project?

A Spring Boot (Kotlin) REST API built specifically as a subject for load, stress, and spike testing research. The app manages **Movies** and **Users** with JWT authentication. It's intentionally simple — the goal is measuring performance, not building features.

---

## Prerequisites

| Tool | Purpose |
|---|---|
| Docker Desktop | Runs the app, database, and test stack |
| Java 21 | Only needed if running outside Docker |
| Git | Clone the repo |

---

## Get it running in 2 commands

```bash
git clone <repo-url>
cd load-testing
docker compose up --build
```

- App: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## Project structure

```
src/                        # Spring Boot Kotlin source code
scripts/
  app.sh  build|test|run    # build the app, run tests, start locally
  db.sh   start|stop|fresh  # manage Postgres container
k6/                         # load test scripts (smoke, load, stress)
k6-results/                 # test output (HTML reports, JSON)
grafana/                    # Grafana dashboard provisioning
docs/                       # roadmap, version guides, testing plan
compose.yaml                # app + postgres stack
docker-compose.k6.yaml      # k6 + influxdb + grafana stack
```

---

## Common workflows

**Run tests**
```bash
bash scripts/app.sh test
```

**DB only + run app from IDE**
```bash
bash scripts/db.sh start
bash scripts/app.sh run
```

**Wipe DB and start fresh**
```bash
bash scripts/db.sh fresh
```

**Run load tests with live Grafana dashboard**
```bash
# Start monitoring stack
docker compose -f docker-compose.k6.yaml up influxdb grafana -d

# Run a test (pick one)
docker compose -f docker-compose.k6.yaml --profile smoke  up k6-smoke
docker compose -f docker-compose.k6.yaml --profile load   up k6-load
docker compose -f docker-compose.k6.yaml --profile stress up k6-stress
```

Grafana: `http://localhost:3000`

---

## Default credentials

| Account | Username | Password |
|---|---|---|
| Admin user | `admin` | `admin123` |
| Regular user | `user` | `user123` |

Login via `POST /auth/login` — returns a JWT token. Use it as `Authorization: Bearer <token>` on protected endpoints.

---

## CI/CD (GitLab)

Pipeline runs automatically on merge to `master`:

`build → test → migrate → docker (push image)`

Load tests run manually from the GitLab UI or automatically on version tags (e.g. `v1.0.0`). HTML reports are saved as pipeline artifacts for 30 days.

---

## Tear down

```bash
docker compose down -v
docker compose -f docker-compose.k6.yaml down -v
```