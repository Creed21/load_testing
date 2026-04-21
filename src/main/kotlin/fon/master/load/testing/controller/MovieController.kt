package fon.master.load.testing.controller

import fon.master.load.testing.dto.MovieRequest
import fon.master.load.testing.dto.MovieResponse
import fon.master.load.testing.service.MovieService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/movies")
class MovieController(private val service: MovieService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun search(
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) genre: String?,
        @RequestParam(required = false) year: Int?
    ): List<MovieResponse> {
        log.debug("GET /movies title={} genre={} year={}", title, genre, year)
        val results = service.search(title, genre, year)
        log.debug("GET /movies returned {} results", results.size)
        return results
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): MovieResponse {
        log.debug("GET /movies/{}", id)
        return service.findById(id)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    fun create(@RequestBody request: MovieRequest): MovieResponse {
        log.info("POST /movies title={}", request.title)
        return service.create(request)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun update(@PathVariable id: Long, @RequestBody request: MovieRequest): MovieResponse {
        log.info("PUT /movies/{} title={}", id, request.title)
        return service.update(id, request)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    fun delete(@PathVariable id: Long) {
        log.info("DELETE /movies/{}", id)
        service.delete(id)
    }
}
