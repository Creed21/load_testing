package fon.master.load.testing.controller

import fon.master.load.testing.dto.LoginRequest
import fon.master.load.testing.dto.LoginResponse
import fon.master.load.testing.service.AuthService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): LoginResponse = authService.login(request)
}
