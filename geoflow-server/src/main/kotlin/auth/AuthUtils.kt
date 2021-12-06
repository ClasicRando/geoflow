package auth

import errors.NoValidSessionException
import errors.UnauthorizedRouteAccessException
import io.ktor.application.ApplicationCall
import io.ktor.request.uri
import io.ktor.sessions.sessions
import io.ktor.sessions.get
import errors.require
import errors.requireOrThrow

/**
 * Utility function that checks to make sure that the underlining call has a session and the user for that session
 * meets the required [role]. If the user is an admin, the specified role is never checked against the user
 */
fun ApplicationCall.requireUserRole(role: String): UserSession {
    val user = sessions.get<UserSession>()
    requireOrThrow<NoValidSessionException>(user != null)
    require(user!!.roles.contains("admin") || role in user.roles) {
        UnauthorizedRouteAccessException(request.uri)
    }
    return user
}
