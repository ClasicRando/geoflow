import auth.UserSession
import html.index
import io.ktor.application.*
import io.ktor.html.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import orm.tables.Actions
import orm.tables.WorkflowOperations

fun Route.index() {
    get("/") {
        call.respondRedirect("/index")
    }
    get("/index") {
        call.respondHtml {
            index()
        }
    }
    post("/index") {
        val params = call.receiveParameters()
        val href = params["href"] ?: "/index"
        call.respondRedirect(href)
    }
}

fun Route.api() {
    get("/api/operations") {
        val operations = runCatching {
            WorkflowOperations.userOperations(call.sessions.get<UserSession>()?.roles!!)
        }.getOrElse { t ->
            call.application.environment.log.error("/api/operations", t)
            listOf()
        }
        call.respond(operations)
    }
    get("/api/actions") {
        val actions = runCatching {
            Actions.userActions(call.sessions.get<UserSession>()?.roles!!)
        }.getOrElse { t ->
            call.application.environment.log.error("/api/actions", t)
            listOf()
        }
        call.respond(actions)
    }
}