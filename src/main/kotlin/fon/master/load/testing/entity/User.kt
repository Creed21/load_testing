package fon.master.load.testing.entity

import jakarta.persistence.*

@Entity
@Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(unique = true, nullable = false)
    val username: String,
    val email: String,
    val password: String,
    @Enumerated(EnumType.STRING)
    val role: Role = Role.USER
)
