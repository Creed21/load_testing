package fon.master.load.testing.dto

data class MovieRequest(
    val title: String,
    val description: String,
    val year: Int,
    val genre: String,
    val director: String,
    val rating: Double? = null
)
