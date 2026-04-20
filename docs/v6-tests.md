# Version 6 — Tests

**Goal:** cover the app at two levels — unit and full integration.

## Test pyramid

```
        [ Integration ]     ← @SpringBootTest, RANDOM_PORT, TestRestTemplate
       [  Unit tests  ]     ← plain JUnit, no Spring context
```

## Dependencies

```xml
<!-- already included by spring-boot-starter-test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<!-- optional: real DB in tests -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

## Examples

```kotlin
// Unit — no Spring
class UserServiceTest {
    private val repo = mockk<UserRepository>()
    private val service = UserService(repo)

    @Test
    fun `create returns saved user`() {
        every { repo.save(any()) } answers { firstArg() }
        val result = service.create("Alice")
        assertEquals("Alice", result.name)
    }
}

// Integration — full Spring context, real beans, no mocks
@SpringBootTest(webEnvironment = RANDOM_PORT)
class UserControllerTest(@Autowired val rest: TestRestTemplate) {

    @Test
    fun `GET users returns seeded list`() {
        val response = rest.getForEntity("/users", Array<UserResponse>::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        assert(response.body!!.size >= 3)
    }

    @Test
    fun `GET users by id returns 404 for missing id`() {
        val response = rest.getForEntity("/users/999", Any::class.java)
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `POST users creates user and returns 201`() {
        val response = rest.postForEntity("/users", UserRequest("Diana", "diana@example.com"), UserResponse::class.java)
        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertNotNull(response.body?.id)
    }
}
```

## Load test focus

Tests are not load tests, but they guard against regressions introduced
when tuning for performance (e.g. removing `@Transactional`, adding caching).
Run the full suite after every load-test-driven change.

## Next step

[Version 7 — Docker + Profiles](v7-docker-profiles.md)
