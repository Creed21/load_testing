package fon.master.load.testing.service

import fon.master.load.testing.dto.UserRequest
import fon.master.load.testing.dto.UserResponse
import fon.master.load.testing.entity.User
import fon.master.load.testing.exception.NotFoundException
import fon.master.load.testing.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun findAll(): List<UserResponse> = userRepository.findAll().map { it.toResponse() }

    fun findById(id: Long): UserResponse =
        userRepository.findById(id).orElseThrow { NotFoundException("User $id not found") }.toResponse()

    fun create(request: UserRequest): UserResponse {
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
