package fon.master.load.testing.service

import fon.master.load.testing.dto.LoginRequest
import fon.master.load.testing.dto.LoginResponse
import fon.master.load.testing.exception.NotFoundException
import fon.master.load.testing.repository.UserRepository
import fon.master.load.testing.security.JwtUtil
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun login(request: LoginRequest): LoginResponse {
        log.info("Login attempt for username={}", request.username)
        val user = userRepository.findByUsername(request.username)
            ?: throw NotFoundException("User '${request.username}' not found")
        require(passwordEncoder.matches(request.password, user.password)) { "Invalid credentials" }
        log.info("Login successful for username={} role={}", user.username, user.role)
        return LoginResponse(jwtUtil.generate(user.username, user.role.name))
    }
}
