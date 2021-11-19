package auth

import io.ktor.auth.Principal
import java.time.Instant

/**
 * Container for session data for the current user. Shares a lot of data with
 * [InternalUsers][database.tables.InternalUsers].
 *
 * **Future Changes**
 * - Decide more appropriate expiration or change to new auth method
 */
data class UserSession(
    val userId: Long,
    val username: String,
    val name: String,
    val roles: List<String>,
    val expiration: Long = Instant.now().plusSeconds(60L * 60).epochSecond,
) : Principal {
    val isExpired: Boolean
        get() = Instant.now().isAfter(Instant.ofEpochSecond(expiration))
    fun hasRole(role: String): Boolean = "admin" in roles || role in roles
}
