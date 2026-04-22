# Version 9 — Nginx Reverse Proxy + Multi-Instance Deployment

**Goal:** run two instances of the Spring Boot app behind an Nginx reverse proxy for round-robin load balancing, and expose a proper observability stack via Spring Boot Actuator on a dedicated management port.

---

## What was built

### 1. Two app instances (`app1`, `app2`)

The single `app` service in `compose.yaml` was split into `app1` and `app2`. Both build from the same `Dockerfile` and share the same Postgres database. Neither exposes a port directly — all traffic enters through nginx.

**Startup ordering** — Liquibase runs migrations once on `app1`. `app2` only starts after `app1` passes its health check, so both instances never race to apply the same changesets:

```yaml
app1:
  healthcheck:
    test: ["CMD-SHELL", "curl -sf http://localhost:8081/actuator/health || exit 1"]
    interval: 10s
    retries: 10
    start_period: 30s

app2:
  depends_on:
    app1:
      condition: service_healthy
```

---

### 2. Nginx reverse proxy (`nginx/nginx.conf`)

Nginx listens on port 80 and round-robins requests across both app instances:

```nginx
upstream spring_apps {
    server app1:8080;
    server app2:8080;
}

server {
    listen 80;

    location / {
        proxy_pass http://spring_apps;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Nginx is `nginx:alpine` and mounts the config as read-only. It depends on both `app1` and `app2`.

**Traffic flow:**
```
Client → localhost:80 → nginx → app1:8080
                              → app2:8080  (round-robin)
                   both → postgres:5432
```

---

### 3. Spring Boot Actuator + Prometheus metrics

**Dependency added to `pom.xml`:**
- `spring-boot-starter-actuator`
- `io.micrometer:micrometer-registry-prometheus`

**Management server runs on a separate port (`8081`)** — completely isolated from the main API on `8080`. The main security filter chain does not apply to it.

```yaml
management:
  server:
    port: 8081
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,loggers
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
```

| Endpoint | URL | Who can see details |
|---|---|---|
| `/actuator/health` | `app:8081` | Status always visible; details only for authenticated users |
| `/actuator/health/liveness` | `app:8081` | Kubernetes liveness probe |
| `/actuator/health/readiness` | `app:8081` | Kubernetes readiness probe |
| `/actuator/prometheus` | `app:8081` | Prometheus scrape target |
| `/actuator/metrics` | `app:8081` | JVM, HTTP, DB metrics |
| `/actuator/loggers` | `app:8081` | View/change log levels at runtime |
| `/actuator/info` | `app:8081` | App name and version |

**Port 8081 is never mapped in `compose.yaml`** — it is internal only (Docker network), reachable by the healthcheck and a Prometheus scraper but not from outside.

**`SecurityConfig` updated** — `/actuator/health` added to the permit list for the main port as well, so if the management port is ever co-located with the main port it still works without credentials.

---

### 4. App info

```yaml
info:
  app:
    name: load-testing
    version: 1.0.0
```

Exposed at `GET /actuator/info` → `{ "app": { "name": "load-testing", "version": "1.0.0" } }`.

---

## Port map

| Port | Mapped | Purpose |
|---|---|---|
| `80` | host → nginx | Public entry point (nginx) |
| `5432` | host → postgres | Direct DB access (dev) |
| `5433` | host → postgres-test | Test DB (dev) |
| `8080` | internal only | Spring Boot main API |
| `8081` | internal only | Spring Boot management (Actuator) |

---

## How to run

```bash
docker compose down -v
docker compose up --build
```

App is available at `http://localhost` (port 80 via nginx).
Actuator health is at `http://localhost:8081/actuator/health` (internal, Docker only).
