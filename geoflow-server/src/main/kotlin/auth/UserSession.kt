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
    /** userOID from the [InternalUsers][database.tables.InternalUsers] table */
    val userId: Long,
    /** unique username for the current user */
    val username: String,
    /** full name of user */
    val name: String,
    /** roles available to the user */
    val roles: List<String>,
    /** token used to call the api */
    val apiToken: String,
) : Principal {
    /** expiration of the session */
    private val expiration: Long = Instant.now().plusSeconds(SESSION_DURATION_SECONDS).epochSecond
    /** getter that checks if the current [Instant] is after the expiration epoch second */
    val isExpired: Boolean
        get() = Instant.now().isAfter(Instant.ofEpochSecond(expiration))

    companion object {
        private const val SESSION_DURATION_SECONDS = 3600L
    }
}
