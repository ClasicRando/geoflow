@file:Suppress("unused")

import auth.UserSession
import database.Database
import database.tables.InternalUsers
import html.errorPage
import html.login
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.sessions.*
import io.ktor.websocket.*
import it.justwrote.kjob.Mongo
import it.justwrote.kjob.job.JobExecutionType
import it.justwrote.kjob.kjob
import mu.KotlinLogging

val logger = KotlinLogging.logger {}
/** Kjob instance used by the server to schedule jobs for the worker application */
val kjob = kjob(Mongo) {
    nonBlockingMaxJobs = 1
    blockingMaxJobs = 1
    maxRetries = 0
    defaultJobExecutor = JobExecutionType.NON_BLOCKING

    exceptionHandler = { t -> logger.error("Unhandled exception", t)}
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
        form("auth-form") {
            userParamName = "username"
            passwordParamName = "password"
            validate { credentials ->
                val validateResult = Database.runWithConnection {
                    InternalUsers.validateUser(it, credentials.name, credentials.password)
                }
                when (validateResult) {
                    is InternalUsers.ValidationResponse.Success -> UserIdPrincipal(credentials.name)
                    is InternalUsers.ValidationResponse.Failure -> {
                        log.info(validateResult.ERROR_MESSAGE)
                        null
                    }
                }
            }
            challenge("/login?message=invalid")
        }
        session<UserSession>("auth-session") {
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
    }
    /** Install session handling. Stores cookies in memory for now. Will change later in development */
    install(Sessions) {
        cookie<UserSession>("user_session", storage = SessionStorageMemory()) {
            cookie.path = "/"
            cookie.extensions["SameSite"] = "lax"
        }
    }
    /** Install JSON serialization to API responses. */
    install(ContentNegotiation) {
        json()
    }
    /** Install WebSockets for pub/sub pattern. */
    install(WebSockets) {

    }
    /** Install exception handling to display standard status pages for defined exception and throwables. */
    install(StatusPages) {
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
    /** Base routing of application */
    routing {
        /** Static Javascript assets. Does not require authentication */
        js()
        /** Group all other non-login pages to require session authentication */
        authenticate("auth-session") {
            index()
            api()
            pipelineStatus()
            pipelineTasks()
            sockets()
        }
        /** Require form authentication while posting to '/login' end point. Collections username and creates session */
        authenticate("auth-form") {
            post("/login") {
                val username = call.principal<UserIdPrincipal>()?.name ?: ""
                val redirect = runCatching {
                    val user = Database.runWithConnection {
                        InternalUsers.getUser(it, username)
                    }
                    call.sessions.set(
                        UserSession(
                            userId = user.userOid,
                            username = username,
                            name = user.name,
                            roles = user.roles//.mapNotNull { it },
                        )
                    )
                    "/index"
                }.getOrElse { t ->
                    log.info("Error session-auth", t)
                    "/login?message=lookup"
                }
                call.respondRedirect(redirect)
            }
        }
        /** Open login to anyone and response with login page. */
        get("/login") {
            call.respondHtml {
                val message = call.request.queryParameters["message"] ?: ""
                login(
                    when(message) {
                        "invalid" -> "Invalid username or password"
                        "lookup" -> "Lookup error for session creation"
                        "expired" -> "Session has expired"
                        "error" -> "Session error. Please login again"
                        else -> ""
                    }
                )
            }
        }
        /** Upon logout request, remove current session and redirect to login. */
        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/login")
        }
    }
}