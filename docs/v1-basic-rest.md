# Version 1 — Basic REST

**Goal:** smallest possible runnable app. Nothing else.

## What you build

- One `@RestController`
- Two endpoints: `GET /users` and `GET /users/{id}`
- Hardcoded in-memory list — no database, no service layer, no validation
- `User` data class defined inline in the controller file

## Folder structure

```
src/main/kotlin/fon/master/load/testing/
└── controller/
    └── UserController.kt
```

## Example controller

```kotlin
data class User(val id: Long, val name: String, val email: String)

@RestController
@RequestMapping("/users")
class UserController {

    private val users = listOf(
        User(1, "Alice", "alice@example.com"),
        User(2, "Bob", "bob@example.com"),
        User(3, "Charlie", "charlie@example.com"),
    )

    @GetMapping
    fun list(): List<User> = users

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): User =
        users.find { it.id == id } ?: throw NoSuchElementException("User $id not found")
}
```

## Load test focus

Baseline throughput with zero business logic. Every later version should
be compared against this number to measure the cost of each added layer.

| Metric | What to observe |
|--------|-----------------|
| RPS | Maximum requests per second before errors appear |
| p99 latency | Worst-case response time under load |
| Error rate | Should be 0% at sane concurrency levels |

## Next step

[Version 2 — Layered architecture](v2-layered-architecture.md)
