@file:Suppress("TooManyFunctions")

import auth.UserSession
import auth.requireUserRole
import database.Database
import database.tables.InternalUsers
import html.adminDashboard
import html.index
import html.login
import html.pipelineStatus
import html.pipelineTasks
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.principal
import io.ktor.html.respondHtml
import io.ktor.http.content.static
import io.ktor.http.content.resources
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.sessions.clear
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import io.ktor.util.getOrFail

/** Open login to anyone and respond with login page. */
fun Route.loginGet() {
    get(path = "/login") {
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

/** POST endpoint for login process. Collects username and creates session */
fun Route.loginPost() {
    post(path = "/login") {
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
                    roles = user.roles
                )
            )
            "/index"
        }.getOrElse { t ->
            call.application.log.info("Error session-auth", t)
            "/login?message=lookup"
        }
        call.respondRedirect(redirect)
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
fun Route.js() {
    static(remotePath = "assets") {
        resources(resourcePackage = "javascript")
    }
}

/** Route for static Icons */
fun Route.icons() {
    static(remotePath = "") {
        resources(resourcePackage = "icons")
    }
}
