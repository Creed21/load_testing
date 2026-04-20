package fon.master.load.testing.controller

import fon.master.load.testing.dto.UserRequest
import fon.master.load.testing.dto.UserResponse
import fon.master.load.testing.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController(private val service: UserService) {

    @GetMapping
    fun list(): List<UserResponse> = service.findAll()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): UserResponse = service.findById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: UserRequest): UserResponse = service.create(request)
}
