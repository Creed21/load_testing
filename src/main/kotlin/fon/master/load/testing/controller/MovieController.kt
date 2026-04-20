package fon.master.load.testing.controller

import fon.master.load.testing.dto.MovieRequest
import fon.master.load.testing.dto.MovieResponse
import fon.master.load.testing.service.MovieService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/movies")
class MovieController(private val service: MovieService) {

    @GetMapping
    fun search(
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) genre: String?,
        @RequestParam(required = false) year: Int?
    ): List<MovieResponse> = service.search(title, genre, year)

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): MovieResponse = service.findById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    fun create(@RequestBody request: MovieRequest): MovieResponse = service.create(request)

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun update(@PathVariable id: Long, @RequestBody request: MovieRequest): MovieResponse =
        service.update(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    fun delete(@PathVariable id: Long) = service.delete(id)
}
