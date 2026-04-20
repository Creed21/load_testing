# Version 3 — Database

**Goal:** replace the in-memory store with real persistence via JPA + PostgreSQL.

## New dependencies (`pom.xml`)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

## Folder structure (additions)

```
src/main/kotlin/fon/master/load/testing/
├── controller/
├── service/
├── repository/
│   └── MovieRepository.kt
├── entity/
│   └── Movie.kt
├── dto/
└── exception/
```

## Key classes

```kotlin
// entity/Movie.kt
@Entity
@Table(name = "movies")
class Movie(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val title: String,
    val description: String,
    val year: Int,
    val genre: String,
    val director: String
)

// repository/MovieRepository.kt
interface MovieRepository : JpaRepository<Movie, Long>
```

## `application.yaml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/loadtest
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
```

## Load test focus

Database I/O is now in the path. Watch for:

| Metric | What to observe |
|--------|-----------------|
| Connection pool exhaustion | HikariCP default pool = 10; tune `maximum-pool-size` |
| p99 latency spike | First sign that DB is the bottleneck |
| Throughput vs v2 | Quantify the cost of persistence |

## Next step

[Version 4 — Validation & error handling](v4-validation-error-handling.md)
