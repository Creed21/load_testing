# Version 4 — Validation & Error Handling

**Goal:** reject bad input early and return consistent error responses.

## New dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

## Folder structure (additions)

```
src/main/kotlin/fon/master/load/testing/
├── exception/
│   ├── NotFoundException.kt
│   ├── ErrorResponse.kt
│   └── GlobalExceptionHandler.kt
└── dto/
    └── UserRequest.kt   ← add constraints here
```

## Key classes

```kotlin
// dto/UserRequest.kt
data class UserRequest(
    @field:NotBlank(message = "name must not be blank")
    @field:Size(max = 100)
    val name: String
)

// exception/ErrorResponse.kt
data class ErrorResponse(val status: Int, val message: String)

// exception/GlobalExceptionHandler.kt
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val msg = ex.bindingResult.fieldErrors.joinToString { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.badRequest().body(ErrorResponse(400, msg))
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException) =
        ResponseEntity.status(404).body(ErrorResponse(404, ex.message ?: "not found"))
}
```

## Load test focus

Send a mix of valid and invalid requests. Validation failures should be
fast (no DB hit). Observe:

| Scenario | Expected |
|----------|----------|
| 100% valid requests | Same throughput as v3 |
| 50% invalid requests | Higher RPS — invalid requests short-circuit DB |
| Error response shape | Always `{ status, message }` — never a stack trace |

## Next step

[Version 5 — Tests](v6-tests.md)
