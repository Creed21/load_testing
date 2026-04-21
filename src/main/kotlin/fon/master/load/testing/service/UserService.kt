package fon.master.load.testing.service

import fon.master.load.testing.dto.UserRequest
import fon.master.load.testing.dto.UserResponse
import fon.master.load.testing.entity.User
import fon.master.load.testing.exception.NotFoundException
import fon.master.load.testing.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun findAll(): List<UserResponse> {
        log.debug("Fetching all users")
        val users = userRepository.findAll().map { it.toResponse() }
        log.debug("Fetched {} users", users.size)
        return users
    }

    fun findById(id: Long): UserResponse {
        log.debug("Finding user by id={}", id)
        return userRepository.findById(id)
            .orElseThrow { NotFoundException("User $id not found") }
            .also { log.debug("Found user id={} username={}", id, it.username) }
            .toResponse()
    }

    fun create(request: UserRequest): UserResponse {
        log.info("Creating user username={} role={}", request.username, request.role)
        val user = User(
            username = request.username,
            email = request.email,
            password = passwordEncoder.encode(request.password)!!,
            role = request.role
        )
        return userRepository.save(user).toResponse()
    }

    private fun User.toResponse() = UserResponse(id!!, username, email, role.name)
}
