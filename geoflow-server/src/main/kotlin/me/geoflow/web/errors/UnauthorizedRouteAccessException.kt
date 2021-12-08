package me.geoflow.web.errors

/** Exception thrown when a user requests a [route] that they are not authorized to access. */
class UnauthorizedRouteAccessException(
    /** route that denied access to the current user */
    val route: String,
): Throwable()
