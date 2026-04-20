package fon.master.load.testing.service

import fon.master.load.testing.dto.UserRequest
import fon.master.load.testing.entity.Role
import fon.master.load.testing.entity.User
import fon.master.load.testing.exception.NotFoundException
import fon.master.load.testing.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceTest {

    private val userRepository: UserRepository = mock()
    private val passwordEncoder: PasswordEncoder = mock()
    private val service = UserService(userRepository, passwordEncoder)

    private val alice = User(id = 1L, username = "alice", email = "alice@example.com", password = "hashed", role = Role.USER)

    @Test
    fun `findAll returns all users`() {
        whenever(userRepository.findAll()).thenReturn(listOf(alice))
        val result = service.findAll()
        assertEquals(1, result.size)
        assertEquals("alice", result[0].username)
    }

    @Test
    fun `findById returns correct user`() {
        whenever(userRepository.findById(1L)).thenReturn(java.util.Optional.of(alice))
        val result = service.findById(1L)
        assertEquals("alice", result.username)
    }

    @Test
    fun `findById throws NotFoundException for missing id`() {
        whenever(userRepository.findById(999L)).thenReturn(java.util.Optional.empty())
        assertThrows<NotFoundException> { service.findById(999L) }
    }

    @Test
    fun `create encodes password and saves user`() {
        whenever(passwordEncoder.encode("secret")).thenReturn("hashed")
        whenever(userRepository.save(any())).thenReturn(alice)
        val result = service.create(UserRequest("alice", "alice@example.com", "secret"))
        assertEquals("alice", result.username)
    }
}
