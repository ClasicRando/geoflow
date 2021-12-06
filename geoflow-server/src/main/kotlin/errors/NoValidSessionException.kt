package errors

/**
 * Exception thrown when the [ApplicationCall][io.ktor.application.ApplicationCall] does not have a valid
 * [UserSession][auth.UserSession]
 */
class NoValidSessionException: Throwable()
