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
import io.ktor.response.respondRedirect
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.sessions.SessionStorageMemory
import io.ktor.websocket.WebSockets
import it.justwrote.kjob.KJob
import it.justwrote.kjob.Mongo
import it.justwrote.kjob.job.JobExecutionType
import it.justwrote.kjob.kjob
import mu.KLogger
import mu.KotlinLogging
import org.slf4j.Logger

/** logger used for the KJob instance */
val logger: KLogger = KotlinLogging.logger {}
/** Kjob instance used by the server to schedule jobs for the worker application */
@Suppress("MagicNumber")
val kjob: KJob = kjob(Mongo) {
    nonBlockingMaxJobs = 1
    blockingMaxJobs = 1
    maxRetries = 0
    defaultJobExecutor = JobExecutionType.NON_BLOCKING

    exceptionHandler = { t -> logger.error("Unhandled exception", t) }
    keepAliveExecutionPeriodInSeconds = 60
    jobExecutionPeriodInSeconds = 1
    cleanupPeriodInSeconds = 300
    cleanupSize = 50

    connectionString = "mongodb://127.0.0.1:27017"
    databaseName = "kjob"
    jobCollection = "kjob-jobs"
    lockCollection = "kjob-locks"
    expireLockInMinutes = 5L
}.start()

/** */
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
        val redirect = when {
            session == null -> "/login"
            session.isExpired -> "/login?message=expired"
            else -> "/login?message=error"
        }
        call.respondRedirect(redirect)
    }
}

/** */
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
        }
        login()
        logout()
    }
}
