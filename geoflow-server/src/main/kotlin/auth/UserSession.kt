package auth

import io.ktor.auth.*
import java.time.Instant

data class UserSession(
    val username: String,
    val name: String,
    val roles: List<String>,
    val expiration: Long
): Principal {
    val isExpired: Boolean
        get() = Instant.now().isAfter(Instant.ofEpochSecond(expiration))
}