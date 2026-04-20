package fon.master.load.testing.entity

import jakarta.persistence.*

@Entity
@Table(name = "movies")
class Movie(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val title: String,
    val description: String,
    val year: Int,
    val genre: String,
    val director: String,
    val rating: Double? = null
)
