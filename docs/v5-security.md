# V5 – Security

## What changed

### Method-level authorization with `@PreAuthorize`

Instead of encoding role rules inside `SecurityConfig`, write-access routes are now guarded directly on the controller methods using `@PreAuthorize("hasRole('ADMIN')")`.

**SecurityConfig** — write routes require authentication only:
```kotlin
.requestMatchers(HttpMethod.POST, "/movies").authenticated()
.requestMatchers(HttpMethod.PUT, "/movies/**").authenticated()
.requestMatchers(HttpMethod.DELETE, "/movies/**").authenticated()
```

**MovieController** — role check lives next to the code it protects:
```kotlin
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
@PreAuthorize("hasRole('ADMIN')")
fun create(...): MovieResponse

@PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
fun update(...): MovieResponse

@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
@PreAuthorize("hasRole('ADMIN')")
fun delete(...)
```

`@EnableMethodSecurity` was added to `SecurityConfig` to activate `@PreAuthorize` support.

## Behaviour (unchanged)

| Caller | Result |
|---|---|
| No token | 401 Unauthorized |
| Valid token, not ADMIN | 403 Forbidden |
| Valid token, ADMIN role | 200 / 201 / 204 |

## Why

Keeping role logic in the controller makes it visible without navigating to the security config. It also makes it easier to apply different rules per method on the same route prefix in future versions.