@file:Suppress("TooManyFunctions")
package me.geoflow.web

import me.geoflow.web.api.makeApiCall
import me.geoflow.web.auth.User
import me.geoflow.web.auth.UserSession
import me.geoflow.web.auth.requireUserRole
import me.geoflow.web.pages.adminDashboard
import me.geoflow.web.pages.index
import me.geoflow.web.pages.login
import me.geoflow.web.pages.pipelineStatus
import me.geoflow.web.pages.pipelineTasks
import me.geoflow.web.pages.dataSources
import me.geoflow.web.api.NoBody
import me.geoflow.web.api.UserResponse
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.html.respondHtml
import io.ktor.http.HttpMethod
import io.ktor.http.content.static
import io.ktor.http.content.resources
import io.ktor.request.path
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

/** Utility to get the [UserSession] for a call */
val ApplicationCall.session: UserSession? get() = this.sessions.get<UserSession>()

/** GET route for login form */
private fun Route.getLogin() {
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
}

/**
 * Validates the user through the API and returns a [Map] to be serialized as a JSON response.
 *
 * If the API validates the user credentials, the user details are requested and a session is created.
 */
private suspend fun PipelineContext<Unit, ApplicationCall>.validateUser(user: User): Map<String, String> {
    val response: Map<String, String> = makeApiCall(
        endPoint = "/login",
        httpMethod = HttpMethod.Post,
        content = user,
    )
    return response["token"]?.let { token ->
        val userResponse = makeApiCall<NoBody, UserResponse>(
            endPoint = "/api/users/self",
            httpMethod = HttpMethod.Get,
            apiToken = token,
        )
        val session = UserSession(
            userId = userResponse.payload.userOid,
            username = userResponse.payload.username,
            name = userResponse.payload.name,
            roles = userResponse.payload.roles,
            apiToken = token,
        )
        call.sessions.set(session)
        mapOf("success" to "true")
    } ?: mapOf("error" to "Incorrect username or password")
}

/**
 * POST route for login form. Validates the user and handles any errors since we do not want to response with an error
 * page
 */
private fun Route.postLogin() {
    post {
        val user = call.receive<User>()
        val response = runCatching {
            validateUser(user)
        }.getOrElse { t ->
            call.application.log.info(call.request.path(), t.message)
            mapOf("error" to t.message)
        }
        call.respond(response)
    }
}

/** Login route  */
fun Route.login() {
    route(path = "/login") {
        getLogin()
        postLogin()
    }
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
            pipelineStatus(code, call)
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

/** */
fun Route.dataSources() {
    get(path = "/data-sources") {
        call.requireUserRole("collection")
        call.respondHtml {
            dataSources(call)
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
