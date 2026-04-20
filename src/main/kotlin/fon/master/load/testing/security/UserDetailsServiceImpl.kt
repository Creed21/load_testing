package fon.master.load.testing.security

import fon.master.load.testing.repository.UserRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(private val userRepository: UserRepository) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails =
        userRepository.findByUsername(username)
            ?.let { u ->
                User.withUsername(u.username)
                    .password(u.password)
                    .roles(u.role.name)
                    .build()
            }
            ?: throw UsernameNotFoundException("User '$username' not found")
}
