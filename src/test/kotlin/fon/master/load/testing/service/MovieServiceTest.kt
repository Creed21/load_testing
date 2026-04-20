package fon.master.load.testing.service

import fon.master.load.testing.dto.MovieRequest
import fon.master.load.testing.entity.Movie
import fon.master.load.testing.exception.NotFoundException
import fon.master.load.testing.repository.MovieRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class MovieServiceTest {

    private val movieRepository: MovieRepository = mock()
    private val service = MovieService(movieRepository)

    private val movie = Movie(id = 1L, title = "Inception", description = "A dream within a dream", year = 2010, genre = "Sci-Fi", director = "Nolan", rating = 8.8)
    private val request = MovieRequest(title = "Inception", description = "A dream within a dream", year = 2010, genre = "Sci-Fi", director = "Nolan", rating = 8.8)

    @Test
    fun `search returns mapped responses`() {
        whenever(movieRepository.search(null, null, null)).thenReturn(listOf(movie))
        val result = service.search(null, null, null)
        assertEquals(1, result.size)
        assertEquals("Inception", result[0].title)
    }

    @Test
    fun `search with filters delegates to repository`() {
        whenever(movieRepository.search("Inception", "Sci-Fi", 2010)).thenReturn(listOf(movie))
        val result = service.search("Inception", "Sci-Fi", 2010)
        assertEquals(1, result.size)
        assertEquals(2010, result[0].year)
    }

    @Test
    fun `search returns empty list when no matches`() {
        whenever(movieRepository.search("Unknown", null, null)).thenReturn(emptyList())
        val result = service.search("Unknown", null, null)
        assertEquals(0, result.size)
    }

    @Test
    fun `findById returns response when movie exists`() {
        whenever(movieRepository.findById(1L)).thenReturn(Optional.of(movie))
        val result = service.findById(1L)
        assertEquals(1L, result.id)
        assertEquals("Inception", result.title)
        assertEquals(8.8, result.rating)
    }

    @Test
    fun `findById throws NotFoundException when movie is missing`() {
        whenever(movieRepository.findById(999L)).thenReturn(Optional.empty())
        assertThrows<NotFoundException> { service.findById(999L) }
    }

    @Test
    fun `create saves movie and returns response`() {
        whenever(movieRepository.save(any())).thenReturn(movie)
        val result = service.create(request)
        assertEquals("Inception", result.title)
        assertEquals("Nolan", result.director)
        assertEquals(8.8, result.rating)
    }

    @Test
    fun `create with no rating saves movie with null rating`() {
        val requestNoRating = request.copy(rating = null)
        val movieNoRating = Movie(id = 1L, title = "Inception", description = "A dream within a dream", year = 2010, genre = "Sci-Fi", director = "Nolan", rating = null)
        whenever(movieRepository.save(any())).thenReturn(movieNoRating)
        val result = service.create(requestNoRating)
        assertEquals(null, result.rating)
    }

    @Test
    fun `update finds movie saves with given id and returns response`() {
        whenever(movieRepository.findById(1L)).thenReturn(Optional.of(movie))
        whenever(movieRepository.save(any())).thenReturn(movie)
        val result = service.update(1L, request)
        assertEquals(1L, result.id)
        assertEquals("Inception", result.title)
    }

    @Test
    fun `update throws NotFoundException when movie is missing`() {
        whenever(movieRepository.findById(999L)).thenReturn(Optional.empty())
        assertThrows<NotFoundException> { service.update(999L, request) }
    }

    @Test
    fun `delete calls deleteById when movie exists`() {
        whenever(movieRepository.existsById(1L)).thenReturn(true)
        service.delete(1L)
        verify(movieRepository).deleteById(1L)
    }

    @Test
    fun `delete throws NotFoundException when movie is missing`() {
        whenever(movieRepository.existsById(999L)).thenReturn(false)
        assertThrows<NotFoundException> { service.delete(999L) }
    }
}