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
@Suppress("MagicNumber")
data class UserSession(
    /** userOID from the [InternalUsers][database.tables.InternalUsers] table */
    val userId: Long,
    /** unique username for the current user */
    val username: String,
    /** full name of user */
    val name: String,
    /** roles available to the user */
    val roles: List<String>,
    /** expiration of the session */
    val expiration: Long = Instant.now().plusSeconds(60L * 60).epochSecond,
) : Principal {
    /** getter that checks if the current [Instant] is after the expiration epoch second */
    val isExpired: Boolean
        get() = Instant.now().isAfter(Instant.ofEpochSecond(expiration))
    /** Checks if provided [role] can be found in [roles] list. Always returns true if user is admin */
    fun hasRole(role: String): Boolean = "admin" in roles || role in roles
}
