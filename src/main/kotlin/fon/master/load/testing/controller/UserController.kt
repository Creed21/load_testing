package fon.master.load.testing.controller

import fon.master.load.testing.dto.UserRequest
import fon.master.load.testing.dto.UserResponse
import fon.master.load.testing.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController(private val service: UserService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun list(): List<UserResponse> {
        log.debug("GET /users")
        val users = service.findAll()
        log.debug("GET /users returned {} users", users.size)
        return users
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): UserResponse {
        log.debug("GET /users/{}", id)
        return service.findById(id)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: UserRequest): UserResponse {
        log.info("POST /users username={}", request.username)
        return service.create(request)
    }
}
