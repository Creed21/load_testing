package fon.master.load.testing.controller

import fon.master.load.testing.dto.MovieRequest
import fon.master.load.testing.dto.MovieResponse
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
class MovieControllerIT {

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
    fun `GET movies returns seeded list without auth`() {
        val response = rest.getForEntity(url("/movies"), Array<MovieResponse>::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        assert(response.body!!.isNotEmpty())
    }

    @Test
    fun `GET movies search by title returns matching results`() {
        val response = rest.getForEntity(url("/movies?title=inception"), Array<MovieResponse>::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Inception", response.body!![0].title)
    }

    @Test
    fun `GET movies by id returns movie without auth`() {
        val allMovies = rest.getForEntity(url("/movies"), Array<MovieResponse>::class.java).body!!
        val firstId = allMovies[0].id
        val response = rest.getForEntity(url("/movies/$firstId"), MovieResponse::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body?.title)
    }

    @Test
    fun `GET movies by id returns 404 for missing id`() {
        val response = rest.getForEntity(url("/movies/99999"), Any::class.java)
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `POST movies creates movie with admin token`() {
        val request = MovieRequest("Test Movie", "A test description", 2024, "Drama", "Test Director")
        val response = rest.exchange(url("/movies"), HttpMethod.POST, HttpEntity(request, adminHeaders()), MovieResponse::class.java)
        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertNotNull(response.body?.id)
        assertEquals("Test Movie", response.body?.title)
    }

    @Test
    fun `POST movies without token returns 401`() {
        val request = MovieRequest("Unauthorized", "No token", 2024, "Drama", "Nobody")
        val response = rest.postForEntity(url("/movies"), request, Any::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `DELETE movie without admin token returns 401`() {
        val response = rest.exchange(url("/movies/1"), HttpMethod.DELETE, HttpEntity.EMPTY, Any::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }
}