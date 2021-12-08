package me.geoflow.web.errors

/**
 * Exception thrown when the [ApplicationCall][io.ktor.application.ApplicationCall] does not have a valid
 * [UserSession][me.geoflow.web.auth.UserSession]
 */
class NoValidSessionException: Throwable()
