import auth.UserSession
import html.login
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.sessions.*
import orm.tables.InternalUsers
import java.time.Instant

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

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
            challenge("/login?message=expired")
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
    routing {
        authenticate("auth-session") {
            index()
        }
        authenticate("auth-form") {
            post("/login") {
                val username = call.principal<UserIdPrincipal>()?.name ?: ""
                val redirect = runCatching {
                    val user = InternalUsers.getUser(username)
                    call.sessions.set(
                        UserSession(
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
                        else -> ""
                    }
                )
            }
        }
    }
}