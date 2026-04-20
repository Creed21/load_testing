# Spring Boot Load Testing — Version Roadmap

> **Additional docs:** [Use Cases](use-cases.md) | [Testing Plan](testing-plan.md)

Each version introduces one concept so nothing is mixed together.

| Version | Focus |
|---------|-------|
| [v1](v1-basic-rest.md) | Plain REST — single controller, hardcoded response |
| [v2](v2-layered-architecture.md) | Layered architecture — controller / service / repository / entity / dto |
| [v3](v3-database.md) | Database — JPA + PostgreSQL, real persistence |
| [v4](v4-validation-error-handling.md) | Validation & error handling — Bean Validation, global exception handler |
| [v5](v6-tests.md) | Tests — unit and integration |
| [v6](v7-docker-profiles.md) | Docker + profiles — Dockerfile, docker-compose, dev/prod profiles |
| [v7](v7-docker-profiles.md) | Docker + profiles — Dockerfile, docker-compose, dev/prod profiles |

## Stack

- **Language:** Kotlin
- **Framework:** Spring Boot (current stable from [Spring Initializr](https://start.spring.io))
- **Build:** Maven
- **Database:** PostgreSQL

## Spring Initializr dependencies (add progressively)

```
Spring Web          ← v1
Spring Data JPA     ← v3
PostgreSQL Driver   ← v3
Validation          ← v4
Lombok / Kotlin     ← throughout
```

## Recommended order rationale

Start with the thinnest possible app and layer one concern at a time.
That way each load-test run isolates a single variable — you can measure
exactly what JPA and validation add to your latency/throughput.