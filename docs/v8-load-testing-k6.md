# Version 8 — k6 Load & Stress Testing + Observability

**Goal:** stress-test the fully containerised app with realistic traffic, stream live metrics into Grafana, and publish HTML reports as GitLab CI artifacts.

---

## What was built

### 1. 100 000-movie seed (`005-seed-100k-movies.xml`)

A single Liquibase changeset that inserts 100 000 dummy movies using PostgreSQL's `generate_series`. It runs once on first startup (preCondition guard prevents re-seeding).

```xml
<sql splitStatements="false">
    INSERT INTO movies (title, description, year, genre, director, rating)
    SELECT
        'Movie ' || i,
        'Auto-generated movie number ' || i,
        1950 + (i % 75),
        (ARRAY['Action','Comedy','Drama','Sci-Fi',...])[( i % 10) + 1],
        'Director ' || ((i % 200) + 1),
        ROUND((RANDOM() * 9 + 1)::NUMERIC, 1)
    FROM generate_series(1, 100000) AS s(i);
</sql>
```

---

### 2. Structured logging (`logback-spring.xml`)

Profile-aware logging added to all controllers and services.

| Profile | Our code | Framework | Output |
|---------|----------|-----------|--------|
| `dev` / `test` | `DEBUG` | `INFO` | Console |
| `prod` | `INFO` | `WARN` | Console + rolling file (`logs/`) |

Key log points added:
- **Controllers** — DEBUG on every request, INFO on mutations
- **MovieService** — timing on `search()` (`completed in Xms, found N results`)
- **AuthService** — INFO on login attempts and outcomes

---

### 3. k6 test suite (`k6/`)

Three scripts, each with a different purpose:

#### `smoke-test.js` — sanity check
- 1 VU · 30 s
- Login → `GET /movies` → `GET /movies/1`
- Strict thresholds: p95 < 500 ms, error rate < 1 %
- Run before every heavier test

#### `load-test.js` — realistic concurrent load
- 90 VUs · 5 min · 4 scenarios running in parallel

| Scenario | VUs | Endpoint | Threshold (p95) |
|----------|-----|----------|-----------------|
| `list_movies` | 30 | `GET /movies` (no filter) | < 10 000 ms |
| `search_movies` | 20 | `GET /movies?genre=X` | < 2 000 ms |
| `get_movie` | 30 | `GET /movies/{id}` | < 300 ms |
| `login` | 10 | `POST /auth/login` | < 500 ms |

#### `stress-test.js` — find the breaking point
- All traffic → `GET /movies` (worst-case: 100 k rows, no pagination)
- Ramping stages: 0 → 50 → 150 → 300 → 0 VUs over 10 min
- Looser thresholds (< 10 % error rate) — the point is to observe degradation

---

### 4. HTML report generator (`k6/html-report.js`)

Shared module imported by all three scripts via `handleSummary`. Generates a self-contained HTML page with:
- Summary cards: total requests, throughput, error rate, max VUs, data received
- Response time table: avg / p50 / p90 / p95 / p99 / max
- Per-scenario p95 breakdown (load test)
- Threshold pass/fail table

---

### 5. Observability stack (`docker-compose.k6.yaml`)

```
InfluxDB 1.8  ← k6 streams metrics here in real-time (--out influxdb=...)
Grafana 10.4  ← reads InfluxDB, shows live dashboard (import ID 2587)
```

Each k6 service is tagged with `--tag testname=smoke/load/stress` so runs can be filtered in Grafana.

---

### 6. GitLab CI — `load-test` stage

The stage:
1. Installs k6 from the official apt repo
2. Starts the Spring Boot JAR in the background with a fresh postgres service
3. Waits for the app to respond on `/movies`
4. Runs `scripts/load-test.sh` (smoke → load → stress in sequence)
5. Stores HTML reports as artifacts exposed as **"Load Test Reports"** on the pipeline page

Trigger modes:
- **Manual** — from GitLab UI → CI/CD → Pipelines → Run pipeline
- **Automatic** — on version tags (`v1.0.0`, `v1.2.3`, …)

---

## Bottlenecks discovered

### Load test results (90 VUs, 5 min)

| Scenario | p95 actual | Threshold | Verdict |
|----------|-----------|-----------|---------|
| `list_movies` | **3 138 ms** | < 10 000 ms | ✓ passes threshold, but 3 s is severe |
| `search_movies` | **2 215 ms** | < 2 000 ms | ✗ failed |
| `get_movie` | **2 133 ms** | < 300 ms | ✗ failed badly |
| `login` | **2 377 ms** | < 500 ms | ✗ failed |
| Data transferred | **53 GB** in 5 min | — | root cause |

### Root cause analysis

1. **No pagination on `GET /movies`** — every call serialises all 100 006 rows into JSON.
   With 30 concurrent VUs each doing this, the JVM heap fills with `List<MovieResponse>` objects and the DB connection pool backs up. Every other endpoint queues behind these giant responses — that is why even `GET /movies/{id}` (which should take ~3 ms) shows 2 133 ms p95.

2. **No index on `genre` column** — `GET /movies?genre=Action` performs a full sequential scan of 100 006 rows every time.

3. **BCrypt on every login** — intentionally slow by design (cost factor 10). At 10 concurrent login VUs, CPU becomes a bottleneck.

### Recommended fixes (future versions)

| Problem | Fix |
|---------|-----|
| `GET /movies` returns all rows | Add `Pageable` parameter, default page size 20 |
| `genre` full scan | `CREATE INDEX idx_movies_genre ON movies(genre)` |
| `title` LIKE scan | Add `pg_trgm` index for trigram similarity search |
| BCrypt under load | Cache JWT tokens; add rate limiting on `/auth/login` |
