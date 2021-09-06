import auth.UserSession
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import kotlinx.html.*
import database.DatabaseConnection
import database.roles
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import org.ktorm.entity.filter
import orm.tables.InternalUsers
import java.time.Instant

fun HTML.index() {
    head {
        title("Hello from Ktor!")
    }
    body {
        div {
            +"Hello from Ktor"
        }
        for (role in DatabaseConnection.database.roles) {
            p {
                +role.name
            }
        }
    }
}

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
                    log.debug(validateResult.message)
                    null
                }
            }
        }
        session<UserSession>("auth-session") {
            validate { session ->
                if (session.isExpired) {
                    log.debug("Session for ${session.username} expired")
                    null
                } else {
                    session
                }
            }
            challenge("/login")
        }
    }
    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.path = "/"
        }
    }
    install(ContentNegotiation) {
        jackson()
    }
    routing {
        authenticate("auth-session") {

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
                            expiration = Instant.now().plusSeconds(60).epochSecond
                        )
                    )
                    "/index"
                }.getOrElse { t ->
                    log.trace("Error session-auth", t)
                    "/login"
                }
                call.respondRedirect(redirect)
            }
        }
        get("/") {
            call.respondHtml(HttpStatusCode.OK, HTML::index)
        }
    }
}