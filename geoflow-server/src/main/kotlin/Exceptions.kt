@file:Suppress("MatchingDeclarationName")

import kotlin.reflect.full.primaryConstructor

/** Exception thrown when a user requests a [route] that they are not authorized to access. */
class UnauthorizedRouteAccessException(
    /** route that denied access to the current user */
    val route: String,
): Throwable()

/**
 * Exception thrown when the [ApplicationCall][io.ktor.application.ApplicationCall] does not have a valid
 * [UserSession][auth.UserSession]
 */
class NoValidSessionException: Throwable()

/** Require contract that throws any [Throwable] the user wants by providing a lambda that returns any Throwable. */
fun require(value: Boolean, block: () -> Throwable) {
    if (!value) {
        val error = block()
        throw error
    }
}

/** */
inline fun <reified T: Throwable> requireOrThrow(value: Boolean) {
    if (!value) {
        val constructor = T::class.primaryConstructor
        requireNotNull(constructor) { "Throwable must have constructor" }
        requireEmpty(constructor.parameters) { "Throwable's constructor must accept 0 parameters" }
        throw constructor.call()
    }
}
