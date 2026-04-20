package fon.master.load.testing.dto

import fon.master.load.testing.entity.Role

data class UserRequest(
    val username: String,
    val email: String,
    val password: String,
    val role: Role = Role.USER
)
