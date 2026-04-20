package fon.master.load.testing.service

import fon.master.load.testing.dto.LoginRequest
import fon.master.load.testing.dto.LoginResponse
import fon.master.load.testing.exception.NotFoundException
import fon.master.load.testing.repository.UserRepository
import fon.master.load.testing.security.JwtUtil
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil
) {
    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByUsername(request.username)
            ?: throw NotFoundException("User '${request.username}' not found")
        require(passwordEncoder.matches(request.password, user.password)) { "Invalid credentials" }
        return LoginResponse(jwtUtil.generate(user.username, user.role.name))
    }
}
