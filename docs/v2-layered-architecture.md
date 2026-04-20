# Version 2 — Layered Architecture

**Goal:** introduce proper package structure without adding a real database yet.

## What you build

- Controller → Service split
- DTOs for request/response shapes
- In-memory data (a `MutableList` or similar) — no JPA yet
- Package layout that all later versions keep

## Folder structure

```
src/main/kotlin/fon/master/load/testing/
├── controller/
│   └── UserController.kt
├── service/
│   └── UserService.kt
├── dto/
│   ├── UserRequest.kt
│   └── UserResponse.kt
└── exception/
    └── NotFoundException.kt
```

## Key classes

```kotlin
// dto/UserResponse.kt
data class UserResponse(val id: Long, val name: String)

// service/UserService.kt
@Service
class UserService {
    private val store = mutableMapOf<Long, String>()
    private var seq = 1L

    fun create(name: String): UserResponse {
        val id = seq++
        store[id] = name
        return UserResponse(id, name)
    }

    fun findAll(): List<UserResponse> =
        store.map { (id, name) -> UserResponse(id, name) }
}

// controller/UserController.kt
@RestController
@RequestMapping("/users")
class UserController(private val service: UserService) {

    @PostMapping
    fun create(@RequestBody req: UserRequest) = service.create(req.name)

    @GetMapping
    fun list() = service.findAll()
}
```

## Load test focus

Measure the overhead of the service layer and object mapping compared to v1.
The delta should be near-zero — if it isn't, something is wrong architecturally.

## Next step

[Version 3 — Database](v3-database.md)
