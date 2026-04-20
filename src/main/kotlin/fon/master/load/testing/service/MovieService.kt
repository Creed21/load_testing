package fon.master.load.testing.service

import fon.master.load.testing.dto.MovieRequest
import fon.master.load.testing.dto.MovieResponse
import fon.master.load.testing.entity.Movie
import fon.master.load.testing.exception.NotFoundException
import fon.master.load.testing.repository.MovieRepository
import org.springframework.stereotype.Service

@Service
class MovieService(private val movieRepository: MovieRepository) {

    fun search(title: String?, genre: String?, year: Int?): List<MovieResponse> =
        movieRepository.search(title, genre, year).map { it.toResponse() }

    fun findById(id: Long): MovieResponse =
        movieRepository.findById(id).orElseThrow { NotFoundException("Movie $id not found") }.toResponse()

    fun create(request: MovieRequest): MovieResponse =
        movieRepository.save(request.toEntity()).toResponse()

    fun update(id: Long, request: MovieRequest): MovieResponse {
        movieRepository.findById(id).orElseThrow { NotFoundException("Movie $id not found") }
        return movieRepository.save(request.toEntity(id)).toResponse()
    }

    fun delete(id: Long) {
        if (!movieRepository.existsById(id)) throw NotFoundException("Movie $id not found")
        movieRepository.deleteById(id)
    }

    private fun Movie.toResponse() = MovieResponse(id!!, title, description, year, genre, director, rating)
    private fun MovieRequest.toEntity(id: Long? = null) = Movie(id, title, description, year, genre, director, rating)
}
