@file:Suppress("unused")
package me.geoflow.web

import me.geoflow.web.api.data
import me.geoflow.web.auth.UserSession
import me.geoflow.web.errors.UnauthorizedRouteAccessException
import me.geoflow.web.pages.errorPage
import io.ktor.application.call
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.session
import io.ktor.auth.Authentication
import io.ktor.auth.SessionAuthenticationProvider
import io.ktor.auth.authenticate
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.features.MissingRequestParameterException
import io.ktor.html.respondHtml
import io.ktor.request.uri
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.sessions.SessionStorageMemory
import io.ktor.websocket.WebSockets
import org.slf4j.Logger

/** Handles session authentication. Validation just checks if the current session has expired */
private fun SessionAuthenticationProvider.Configuration<UserSession>.configure(log: Logger) {
    validate { session ->
        if (session.isExpired) {
            log.info("Session for ${session.username} expired")
            null
        } else {
            session
        }
    }
    challenge { session ->
        if (call.request.uri.contains("/data/")) {
            val message = when {
                session == null -> "No session found"
                session.isExpired -> "Session for ${session.username} has expired"
                else -> "Error while trying to acquire session"
            }
            call.respondText("$message. Please Login to continue")
        } else {
            val redirect = when {
                session == null -> "/login"
                session.isExpired -> "/login?message=expired"
                else -> "/login?message=error"
            }
            call.respondRedirect(redirect)
        }
    }
}

/** Configuration of the status pages for specific exceptions */
private fun StatusPages.Configuration.configure() {
    exception<MissingRequestParameterException> { cause ->
        call.respondHtml {
            errorPage("Missing parameter ${cause.parameterName} in url. ${cause.message}")
        }
    }
    exception<UnauthorizedRouteAccessException> { cause ->
        call.respondHtml {
            errorPage("The current user does not have access to the desired route, ${cause.route}")
        }
    }
    exception<Throwable> { cause ->
        call.respondHtml {
            errorPage(cause.message ?: "")
        }
    }
}

/** Main entry of the server application. Initializes the server engine. */
fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

/** Module describing server features and routing */
@Suppress("unused")
fun Application.module() {
    /**
     * Install form and session based authentication methods. Form used for initial login and session used in subsequent
     * requests
     */
    install(Authentication) {
        session<UserSession>("auth-session") {
            configure(log)
        }
    }
    /** Install session handling. Stores cookies in memory for now. Will change later in development */
    install(Sessions) {
        cookie<UserSession>(name = "user_session", storage = SessionStorageMemory()) {
            cookie.path = "/"
            cookie.extensions["SameSite"] = "lax"
        }
    }
    /** Install JSON serialization to API responses. */
    install(ContentNegotiation) {
        json()
    }
    /** Install WebSockets for pub/sub pattern. */
    install(WebSockets) { }
    /** Install exception handling to display standard status pages for defined exceptions and throwables. */
    install(StatusPages) {
        configure()
    }
    /** Base routing of application */
    routing {
        /** Static resources. Does not require authentication */
        assets()
        /** Group all other non-login pages to require session authentication */
        authenticate("auth-session") {
            index()
            data()
            pipelineStatus()
            pipelineTasks()
            adminDashboard()
            dataSources()
        }
        login()
        logout()
    }
}
