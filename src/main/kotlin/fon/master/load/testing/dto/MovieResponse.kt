package fon.master.load.testing.dto

data class MovieResponse(
    val id: Long,
    val title: String,
    val description: String,
    val year: Int,
    val genre: String,
    val director: String,
    val rating: Double?
)
