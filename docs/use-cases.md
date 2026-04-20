# Use Cases

Subject app: Spring Boot (Kotlin) load testing project.  
All use cases operate on the `User` resource and are kept intentionally simple so that each version of the app adds exactly one variable to the load test.

---

## UC1 — List All Users

| Field | Value |
|-------|-------|
| **Endpoint** | `GET /users` |
| **Actor** | Load test client / API consumer |
| **Precondition** | App is running; in-memory store contains at least the 3 seeded users (v1–v2) or DB has rows (v3+) |
| **Flow** | Client sends `GET /users` → controller calls `service.findAll()` → returns JSON array |
| **Success response** | `200 OK` + `[{"id":1,"name":"Alice","email":"alice@example.com"}, ...]` |
| **Failure response** | `500 Internal Server Error` (unexpected/general exception) |

---

## UC2 — Get User by ID

| Field | Value |
|-------|-------|
| **Endpoint** | `GET /users/{id}` |
| **Actor** | Load test client / API consumer |
| **Precondition** | App is running |
| **Flow** | Client sends `GET /users/1` → controller calls `service.findById(1)` → returns single user JSON |
| **Success response** | `200 OK` + `{"id":1,"name":"Alice","email":"alice@example.com"}` |
| **Failure response — not found** | `404 Not Found` when `id` does not exist (`NotFoundException` mapped by global exception handler) |
| **Failure response — general** | `500 Internal Server Error` (unexpected exception) |

---

## UC3 — Create a User

| Field | Value |
|-------|-------|
| **Endpoint** | `POST /users` |
| **Actor** | Load test client / API consumer |
| **Precondition** | App is running |
| **Request body** | `{"name":"Diana","email":"diana@example.com"}` |
| **Flow** | Client sends `POST /users` with JSON body → controller calls `service.create(request)` → persists and returns new user |
| **Success response** | `201 Created` + `{"id":4,"name":"Diana","email":"diana@example.com"}` |
| **Failure response — invalid input** | `400 Bad Request` (Bean Validation, v4+) |
| **Failure response — general** | `500 Internal Server Error` (unexpected exception) |

---

## Use Case Evolution per Version

| Version | UC1 List | UC2 Get by ID | UC3 Create |
|---------|----------|---------------|------------|
| v1 | Hardcoded list, no service | Not available | Not available |
| v2 | In-memory `MutableMap` | `404` via `NotFoundException` + global handler | In-memory, returns `201` |
| v3 | JPA + PostgreSQL query | JPA `findById`, `404` on missing | JPA `save` |
| v4 | Same as v3 | `404` on missing ID | `400` on invalid input, `500` on general error |
| v5 | Covered by unit + integration tests | Covered by unit + integration tests | Covered by unit + integration tests |
| v6 | Docker + profile-driven config | Docker + profile-driven config | Docker + profile-driven config |
