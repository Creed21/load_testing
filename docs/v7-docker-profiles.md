# Version 7 — Docker + Profiles

**Goal:** containerise the app and separate dev/prod config with Spring profiles.

## Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:

```bash
./mvnw clean package -DskipTests
docker build -t load-testing-app .
docker run -p 8080:8080 --env SPRING_PROFILES_ACTIVE=prod load-testing-app
```

## docker-compose.yaml

```yaml
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/loadtest
    depends_on:
      db:
        condition: service_healthy

  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: loadtest
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 3s
      retries: 5
```

## Spring profiles

```
src/main/resources/
├── application.yaml          ← shared config
├── application-dev.yaml      ← local dev overrides (show-sql: true)
└── application-prod.yaml     ← prod overrides (PostgreSQL, show-sql: false)
```

```yaml
# application-dev.yaml
spring:
  jpa:
    show-sql: true

# application-prod.yaml
spring:
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate
```

## Load test focus

With everything containerised you can now run realistic load tests:

| Scenario | What to vary |
|----------|-------------|
| Single container | Baseline Docker overhead vs bare JVM |
| Scale app replicas | `docker compose up --scale app=3` + a load balancer |
| Resource limits | `mem_limit: 512m` — find OOM threshold |
| Prod vs dev profile | Confirm `show-sql: true` tanks throughput in dev |

## What's next

The app is now a complete, production-shaped service. Possible directions:

- Add a load balancer (Nginx or Traefik) in front of scaled replicas
- Add Prometheus + Grafana for real-time metrics during tests
- Try async endpoints (`suspend fun` / `@Async`) and compare throughput