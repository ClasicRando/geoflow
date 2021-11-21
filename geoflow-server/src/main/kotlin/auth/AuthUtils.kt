package auth

import NoValidSessionException
import UnauthorizedRouteAccessException
import io.ktor.application.ApplicationCall
import io.ktor.request.uri
import io.ktor.sessions.sessions
import io.ktor.sessions.get
import require
import requireOrThrow

/** */
fun ApplicationCall.requireUserRole(role: String) {
    val user = sessions.get<UserSession>()
    requireOrThrow<NoValidSessionException>(user != null)
    require(user!!.roles.contains("admin") || role in user.roles) {
        UnauthorizedRouteAccessException(request.uri)
    }
}
