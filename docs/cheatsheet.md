# Load Testing — Cheat Sheet

Quick reference for running the full stack locally and in GitLab CI.

---

## Prerequisites

- Docker Desktop running
- GitLab project with `DB_PASSWORD` set as a masked CI/CD variable

---

## Local — start / stop

```bash
# Start app + postgres (rebuild image if code changed)
docker compose up --build -d

# Start app + postgres + InfluxDB + Grafana
docker compose -f compose.yaml -f docker-compose.k6.yaml up -d

# Stop everything and wipe volumes (fresh DB on next start)
docker compose down -v
```

| Service | URL |
|---------|-----|
| App (Swagger UI) | http://localhost:8080/swagger-ui.html |
| Grafana | http://localhost:3000 |
| InfluxDB | http://localhost:8086 |

---

## Grafana dashboard (first time only)

1. Open http://localhost:3000
2. Left sidebar **"+"** → **Import**
3. Enter `2587` → **Load** → select **InfluxDB-k6** → **Import**

---

## Run k6 tests locally

> The app must already be running before you run any k6 test.

```bash
# Smoke test  (1 VU · 30 s — sanity check)
docker compose -f compose.yaml -f docker-compose.k6.yaml --profile smoke run --rm k6-smoke

# Load test   (90 VUs · 5 min · 4 scenarios)
docker compose -f compose.yaml -f docker-compose.k6.yaml --profile load run --rm k6-load

# Stress test (ramps to 300 VUs · 10 min)
docker compose -f compose.yaml -f docker-compose.k6.yaml --profile stress run --rm k6-stress
```

Results are saved to `k6-results/` after each run:

| File | Contents |
|------|----------|
| `smoke-report.html` | Human-readable HTML report |
| `smoke-summary.json` | Full metrics as JSON |
| `smoke-results.json` | Raw per-request NDJSON stream |
| *(same pattern for load / stress)* | |

---

## GitLab CI pipeline

### Trigger options

```bash
# Option A — version tag (auto-runs load-test stage)
git tag v1.0.0
git push origin v1.0.0

# Option B — manual from GitLab UI
# CI/CD → Pipelines → Run pipeline → select branch
```

### Pipeline stages

| Stage | What it does | When |
|-------|-------------|------|
| `build` | `mvn package` → JAR artifact | always |
| `test` | Unit + integration tests | always |
| `migrate` | Liquibase update against postgres | always |
| `docker` | Build & push image to registry | master branch only |
| `load-test` | Smoke → Load → Stress k6 tests | manual or version tag |

### Find the HTML reports in GitLab

```
CI/CD → Pipelines → [your pipeline] → load-test job → "Load Test Reports" button
```

---

## Useful curl commands

```bash
# Login (get JWT token)
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# List movies (no filter — returns all 100 006 rows)
curl -s http://localhost:8080/movies | head -c 500

# Search by genre
curl -s "http://localhost:8080/movies?genre=Action" | head -c 500

# Get single movie
curl -s http://localhost:8080/movies/1

# Create movie (requires admin JWT)
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

curl -s -X POST http://localhost:8080/movies \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"Test","description":"desc","year":2024,"genre":"Drama","director":"Me"}'
```

---

## k6 test files

```
k6/
├── html-report.js    ← shared HTML report generator (imported by all tests)
├── smoke-test.js     ← 1 VU · 30 s · strict thresholds
├── load-test.js      ← 90 VUs · 5 min · 4 concurrent scenarios
└── stress-test.js    ← ramps 0→300 VUs · finds breaking point
```

---

## Known bottlenecks (v8 findings)

| Endpoint | p95 @ 90 VUs | Problem |
|----------|-------------|---------|
| `GET /movies` | ~3 000 ms | No pagination — serialises 100 k rows |
| `GET /movies?genre=X` | ~2 200 ms | No index on `genre` column |
| `GET /movies/{id}` | ~2 100 ms | Queues behind unbounded list requests |
| `POST /auth/login` | ~2 400 ms | BCrypt CPU cost under concurrent load |
