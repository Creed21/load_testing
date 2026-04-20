package fon.master.load.testing.controller

import fon.master.load.testing.dto.UserRequest
import fon.master.load.testing.dto.UserResponse
import fon.master.load.testing.entity.Role
import fon.master.load.testing.security.JwtUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestTemplate
import java.net.URI

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserControllerIT {

    @Value("\${local.server.port}")
    private var port: Int = 0

    @Autowired private lateinit var jwtUtil: JwtUtil

    private val rest = RestTemplate().apply {
        errorHandler = object : ResponseErrorHandler {
            override fun hasError(response: ClientHttpResponse) = false
            override fun handleError(url: URI, method: HttpMethod, response: ClientHttpResponse) {}
        }
    }

    private fun url(path: String) = "http://localhost:$port$path"

    private fun adminHeaders() = HttpHeaders().also {
        it.setBearerAuth(jwtUtil.generate("admin", Role.ADMIN.name))
    }

    @Test
    fun `GET users returns seeded list`() {
        val response = rest.exchange(url("/users"), HttpMethod.GET, HttpEntity<Unit>(adminHeaders()), Array<UserResponse>::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assert(response.body!!.isNotEmpty())
    }

    @Test
    fun `GET users by id returns 404 for missing id`() {
        val response = rest.exchange(url("/users/9999"), HttpMethod.GET, HttpEntity<Unit>(adminHeaders()), Any::class.java)
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `GET users without token returns 401`() {
        val response = rest.getForEntity(url("/users"), Any::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `POST users creates user and returns 201`() {
        val request = UserRequest("testuser", "test@example.com", "pass123")
        val response = rest.exchange(url("/users"), HttpMethod.POST, HttpEntity(request, adminHeaders()), UserResponse::class.java)
        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertNotNull(response.body?.id)
        assertEquals("testuser", response.body?.username)
    }
}