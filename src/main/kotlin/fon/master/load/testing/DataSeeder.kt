package fon.master.load.testing

import fon.master.load.testing.entity.Movie
import fon.master.load.testing.entity.Role
import fon.master.load.testing.entity.User
import fon.master.load.testing.repository.MovieRepository
import fon.master.load.testing.repository.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
@Profile("dev", "test")
@ConditionalOnProperty(name = ["app.seed"], havingValue = "true")
class DataSeeder(
    private val userRepository: UserRepository,
    private val movieRepository: MovieRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    override fun run(vararg args: String) {
        seedUsers()
        seedMovies()
    }

    private fun seedUsers() {
        if (userRepository.count() > 0) return
        userRepository.saveAll(listOf(
            User(username = "admin", email = "admin@loadtest.com", password = passwordEncoder.encode("admin123")!!, role = Role.ADMIN),
            User(username = "user",  email = "user@loadtest.com",  password = passwordEncoder.encode("user123")!!,  role = Role.USER)
        ))
    }

    private fun seedMovies() {
        if (movieRepository.count() > 0) return
        movieRepository.saveAll(listOf(
            Movie(title = "Inception",     description = "A thief who enters the dreams of others.",          year = 2010, genre = "Sci-Fi",   director = "Christopher Nolan"),
            Movie(title = "The Matrix",    description = "A hacker discovers the truth about reality.",       year = 1999, genre = "Sci-Fi",   director = "The Wachowskis"),
            Movie(title = "Interstellar",  description = "A team travels through a wormhole near Saturn.",    year = 2014, genre = "Sci-Fi",   director = "Christopher Nolan"),
            Movie(title = "The Godfather", description = "The aging patriarch of a crime dynasty.",           year = 1972, genre = "Crime",    director = "Francis Ford Coppola"),
            Movie(title = "Parasite",      description = "Greed and class discrimination threaten a family.", year = 2019, genre = "Thriller", director = "Bong Joon-ho"),
            Movie(title = "Pulp Fiction",  description = "The lives of two mob hitmen intertwine.",           year = 1994, genre = "Crime",    director = "Quentin Tarantino")
        ))
    }
}