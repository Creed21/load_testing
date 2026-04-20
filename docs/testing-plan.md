# Testing Plan

This document covers all layers of testing for the Spring Boot (Kotlin) load testing subject app.  
The app evolves through 7 versions — each test type targets the layer being introduced in that version.

---

## Infrastructure

| Component | Detail |
|-----------|--------|
| Language | Kotlin 2.x |
| Framework | Spring Boot 4.x |
| Test runner | JUnit 5 (via `kotlin-test-junit5`) |
| Mocking | Mockito-Kotlin |
| Coverage | JaCoCo (Maven plugin) |
| DB (runtime) | PostgreSQL 16 (via Docker Compose) |
| Container | Docker (multi-stage build) |

---

## 1. Unit Testing

**Goal:** Verify each class in isolation, no Spring context, no DB.

**Scope:** `UserService`, `NotFoundException`, DTO data classes.

**Tool:** JUnit 5 + plain Kotlin instantiation.

**Run:** `mvn test`

### Test cases

| Test | Class | Assertion |
|------|-------|-----------|
| `findAll returns seeded users` | `UserService` | result size == 3 |
| `create adds user and returns it` | `UserService` | name/email match, size == 4 |
| `findById returns correct user` | `UserService` | name == "Alice" for id=1 |
| `findById throws NotFoundException for missing id` | `UserService` | `assertFailsWith<NotFoundException>` |

**Coverage target:** ≥ 80 % line coverage on `service/` package (JaCoCo).

---

## 2. Integration Testing

**Goal:** Verify the full request-response cycle — routing, serialization, exception handling, status codes — with the real Spring context loaded.

**Scope:** `UserController` + `UserService` + exception handler wired together via `@SpringBootTest`.

**Tool:** `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` (no mocks — real beans).

**DB:** PostgreSQL via Docker Compose (v3+); no DB needed for v1–v2 in-memory store.

**Run:** `mvn verify`

### Test cases

| Test | Endpoint | Expected |
|------|----------|----------|
| `GET /users returns seeded list` | `GET /users` | `200`, array size ≥ 3 |
| `GET /users/{id} returns user` | `GET /users/1` | `200`, `name == "Alice"` |
| `GET /users/{id} returns 404 for missing id` | `GET /users/999` | `404 Not Found` |
| `POST /users creates user and returns 201` | `POST /users` | `201`, `id` present in body |
| `POST /users with invalid body returns 400` | `POST /users` (empty body) | `400 Bad Request` (v4+) |
| Unexpected error returns 500 | n/a (triggered via fault injection) | `500 Internal Server Error` |

---

## 3. Load Testing

**Goal:** Establish baseline throughput and latency for each app version under sustained normal load.

**Tool:** [k6](https://k6.io) or [Apache JMeter](https://jmeter.apache.org).

**Target:** App running in Docker (`docker compose up --build`, port `8080`).

### Scenarios

| Scenario | Endpoint | Virtual Users | Duration | Success Criteria |
|----------|----------|---------------|----------|-----------------|
| List users baseline | `GET /users` | 50 | 2 min | p95 < 200 ms, error rate < 1 % |
| Get by ID baseline | `GET /users/1` | 50 | 2 min | p95 < 200 ms, error rate < 1 % |
| Create user baseline | `POST /users` | 20 | 2 min | p95 < 300 ms, error rate < 1 % |

**Run per version** (v1 → v2 → v3 → v4 → v6 → v7) and record delta to isolate layer cost.

---

## 4. Stress Testing

**Goal:** Find the breaking point — max load the app can handle before latency degrades or errors appear.

**Tool:** k6 / JMeter with a ramp-up executor.

**Target:** Same Docker setup.

### Scenario

```
Ramp up:   0 → 200 VU over 5 min
Sustained: 200 VU for 5 min
Ramp down: 200 → 0 VU over 2 min
```

| Endpoint | Metric to watch |
|----------|-----------------|
| `GET /users` | p99 latency, error rate, CPU %, heap |
| `POST /users` | p99 latency, DB connection pool exhaustion (v3+) |

**Pass criteria:** System recovers gracefully after ramp-down (no OOM, no stuck threads).

---

## 5. Spike Testing

**Goal:** Verify the app survives a sudden traffic burst and recovers.

**Tool:** k6 / JMeter with instantaneous VU jump.

### Scenario

```
Baseline:  10 VU for 1 min
Spike:     10 → 500 VU instantly, hold 30 s
Recovery:  500 → 10 VU instantly, hold 2 min
```

| Metric | Expected behaviour |
|--------|--------------------|
| Error rate during spike | < 5 % (503/504 acceptable, not 500) |
| p95 latency during spike | Degrades gracefully, does not timeout |
| p95 latency after spike | Returns to baseline within 60 s |
| No data loss | All `POST /users` requests either succeed or return an explicit error (no silent drops) |

---

## Test Execution Order

```
Unit tests          ← mvn test    (every commit, no Spring context, fast)
Integration tests   ← mvn verify  (every commit, full Spring context, slower)
Load tests          ← manual, per version release
Stress tests        ← manual, after load baseline is stable
Spike tests         ← manual, after stress results are documented
```

---

## Running Tests Locally

```bash
# Unit tests only (no Spring context)
mvn test

# Unit + integration tests with JaCoCo coverage report (target/site/jacoco/index.html)
mvn verify

# Start app for load/stress/spike tests
docker compose up --build

# Tear down
docker compose down -v
```
