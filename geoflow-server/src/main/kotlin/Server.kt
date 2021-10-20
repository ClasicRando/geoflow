@file:Suppress("unused")

import auth.UserSession
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
import orm.tables.InternalUsers
import java.time.Instant

val logger = KotlinLogging.logger {}
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

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    install(Authentication) {
        form("auth-form") {
            userParamName = "username"
            passwordParamName = "password"
            validate { credentials ->
                val validateResult = InternalUsers.validateUser(credentials.name, credentials.password)
                if (validateResult.isSuccess) {
                    UserIdPrincipal(credentials.name)
                } else {
                    log.info(validateResult.message)
                    null
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
    install(Sessions) {
        cookie<UserSession>("user_session", storage = SessionStorageMemory()) {
            cookie.path = "/"
            cookie.extensions["SameSite"] = "lax"
        }
    }
    install(ContentNegotiation) {
        json()
    }
    install(WebSockets) {

    }
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
    routing {
        js()
        authenticate("auth-session") {
            index()
            api()
            pipelineStatus()
            pipelineTasks()
            sockets()
        }
        authenticate("auth-form") {
            post("/login") {
                val username = call.principal<UserIdPrincipal>()?.name ?: ""
                val redirect = runCatching {
                    val user = InternalUsers.getUser(username)
                    call.sessions.set(
                        UserSession(
                            userId = user.userOid,
                            username = username,
                            name = user.name,
                            roles = user.roles.mapNotNull { it },
                            expiration = Instant.now().plusSeconds(60L * 60).epochSecond
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
        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/login")
        }
    }
}