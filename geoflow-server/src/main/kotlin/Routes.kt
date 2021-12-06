@file:Suppress("TooManyFunctions")

import api.makeApiCall
import auth.User
import auth.UserSession
import auth.requireUserRole
import database.Database
import database.tables.InternalUsers
import pages.adminDashboard
import pages.index
import pages.login
import pages.pipelineStatus
import pages.pipelineTasks
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.html.respondHtml
import io.ktor.http.HttpMethod
import io.ktor.http.content.static
import io.ktor.http.content.resources
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.sessions.clear
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import io.ktor.util.getOrFail
import io.ktor.util.pipeline.PipelineContext
import java.sql.Connection

/** */
val ApplicationCall.session: UserSession? get() = this.sessions.get<UserSession>()

/** */
fun Route.login() {
    route(path = "/login") {
        get {
            call.respondHtml {
                val message = call.request.queryParameters["message"] ?: ""
                login(
                    when (message) {
                        "invalid" -> "Invalid username or password"
                        "lookup" -> "Lookup error for session creation"
                        "expired" -> "Session has expired"
                        "error" -> "Session error. Please login again"
                        else -> ""
                    }
                )
            }
        }
        post {
            val user = call.receive<User>()
            val response = Database.runWithConnectionAsync {
                when (val validateResult = InternalUsers.validateUser(it, user.username, user.password)) {
                    is InternalUsers.ValidationResponse.Success -> {
                        handleUserPostValidate(it, user)
                        mapOf("success" to true)
                    }
                    is InternalUsers.ValidationResponse.Failure -> {
                        call.application.log.info(validateResult.ERROR_MESSAGE)
                        mapOf("error" to validateResult.ERROR_MESSAGE)
                    }
                }
            }
            call.respond(response)
        }
    }
}

/** */
private suspend fun PipelineContext<Unit, ApplicationCall>.handleUserPostValidate(
    connection: Connection,
    user: User,
) {
    val internalUser = InternalUsers.getUser(connection, user.username)
    val response: Map<String, String> = makeApiCall(
        endPoint = "/login",
        httpMethod = HttpMethod.Post,
        content = user,
    )
    val session = UserSession(
        userId = internalUser.userOid,
        username = internalUser.username,
        name = internalUser.name,
        roles = internalUser.roles,
        apiToken = response["token"] ?: throw IllegalStateException("Token from api must not be null")
    )
    call.sessions.set(session)
}

/** Upon logout request, remove current session and redirect to login. */
fun Route.logout() {
    get(path = "/logout") {
        call.sessions.clear<UserSession>()
        call.respondRedirect("/login")
    }
}

/** Entry route that handles empty an empty path or the index route. Empty routes are redirected to index. */
fun Route.index() {
    get(path = "/") {
        call.respondRedirect("/index")
    }
    get(path = "/index") {
        call.respondHtml {
            index()
        }
    }
}

/**
 * Pipeline status route that handles all workflow code values.
 *
 * For GET requests, the current user must be authorized to access the workflow code specified in the request. For POST
 * requests, the user is trying to pick up the run specified. Tries to pick up run and throws error (after logging)
 * if operation cannot complete successfully. If successful, the user if redirected to appropriate route.
 */
fun Route.pipelineStatus() {
    get(path = "/pipeline-status/{code}") {
        val code = call.parameters.getOrFail("code")
        call.respondHtml {
            pipelineStatus(code)
        }
    }
}

/** Pipeline tasks route that handles request to view a specific run's task list. */
fun Route.pipelineTasks() {
    get(path = "/tasks/{runId}") {
        call.respondHtml {
            pipelineTasks(call.parameters.getOrFail("runId").toLong())
        }
    }
}

/** Admin Dashboard route for viewing admin details */
fun Route.adminDashboard() {
    get(path = "/admin-dashboard") {
        call.requireUserRole("admin")
        call.respondHtml {
            adminDashboard()
        }
    }
}

/** Route for static Javascript assets */
fun Route.assets() {
    static(remotePath = "/assets") {
        resources(resourcePackage = "javascript")
    }
    static(remotePath = "/") {
        resources(resourcePackage = "icons")
    }
}
