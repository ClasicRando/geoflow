@file:Suppress("MatchingDeclarationName")

/** Exception thrown when a user requests a [route] that they are not authorized to access. */
class UnauthorizedRouteAccessException(
    /** route that denied access to the current user */
    val route: String,
): Throwable()

/** Require contract that throws any [Throwable] the user wants by providing a lambda that returns any Throwable. */
fun require(value: Boolean, block: () -> Throwable) {
    if (!value) {
        val error = block()
        throw error
    }
}
