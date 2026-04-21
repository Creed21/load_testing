package fon.master.load.testing.service

import fon.master.load.testing.dto.MovieRequest
import fon.master.load.testing.dto.MovieResponse
import fon.master.load.testing.entity.Movie
import fon.master.load.testing.exception.NotFoundException
import fon.master.load.testing.repository.MovieRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MovieService(private val movieRepository: MovieRepository) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun search(title: String?, genre: String?, year: Int?): List<MovieResponse> {
        log.debug("Searching movies: title={} genre={} year={}", title, genre, year)
        val start = System.currentTimeMillis()
        val results = movieRepository.search(title, genre, year).map { it.toResponse() }
        log.info("Movie search completed in {}ms, found {} results", System.currentTimeMillis() - start, results.size)
        return results
    }

    fun findById(id: Long): MovieResponse {
        log.debug("Finding movie by id={}", id)
        return movieRepository.findById(id)
            .orElseThrow { NotFoundException("Movie $id not found") }
            .also { log.debug("Found movie id={} title={}", id, it.title) }
            .toResponse()
    }

    fun create(request: MovieRequest): MovieResponse {
        log.info("Creating movie title={}", request.title)
        return movieRepository.save(request.toEntity()).toResponse()
    }

    fun update(id: Long, request: MovieRequest): MovieResponse {
        movieRepository.findById(id).orElseThrow { NotFoundException("Movie $id not found") }
        log.info("Updating movie id={} title={}", id, request.title)
        return movieRepository.save(request.toEntity(id)).toResponse()
    }

    fun delete(id: Long) {
        if (!movieRepository.existsById(id)) throw NotFoundException("Movie $id not found")
        log.info("Deleting movie id={}", id)
        movieRepository.deleteById(id)
    }

    private fun Movie.toResponse() = MovieResponse(id!!, title, description, year, genre, director, rating)
    private fun MovieRequest.toEntity(id: Long? = null) = Movie(id, title, description, year, genre, director, rating)
}
