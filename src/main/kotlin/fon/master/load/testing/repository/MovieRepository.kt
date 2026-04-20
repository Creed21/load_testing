package fon.master.load.testing.repository

import fon.master.load.testing.entity.Movie
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MovieRepository : JpaRepository<Movie, Long> {

    @Query("""
        SELECT * FROM movies m WHERE
        (:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', CAST(:title AS text), '%'))) AND
        (:genre IS NULL OR LOWER(m.genre) = LOWER(CAST(:genre AS text))) AND
        (:year IS NULL OR m.year = :year)
    """, nativeQuery = true)
    fun search(
        @Param("title") title: String?,
        @Param("genre") genre: String?,
        @Param("year") year: Int?
    ): List<Movie>
}
