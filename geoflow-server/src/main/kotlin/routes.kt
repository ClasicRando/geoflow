import auth.UserSession
import html.accessRestricted
import html.index
import html.pipelineStatus
import io.ktor.application.*
import io.ktor.html.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import orm.tables.Actions
import orm.tables.PipelineRuns
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

fun Route.pipelineStatus() {
    get("/collection") {
        call.respondHtml {
            if (call.sessions.get<UserSession>()!!.hasRole("collection")) {
                pipelineStatus("collection")
            } else {
                accessRestricted("collection")
            }
        }
    }
    get("/load") {
        call.respondHtml {
            if (call.sessions.get<UserSession>()!!.hasRole("load")) {
                pipelineStatus("load")
            } else {
                accessRestricted("load")
            }
        }
    }
    get("/check") {
        call.respondHtml {
            if (call.sessions.get<UserSession>()!!.hasRole("check")) {
                pipelineStatus("check")
            } else {
                accessRestricted("check")
            }
        }
    }
    get("/qa") {
        call.respondHtml {
            if (call.sessions.get<UserSession>()!!.hasRole("qa")) {
                pipelineStatus("qa")
            } else {
                accessRestricted("qa")
            }
        }
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
    get("api/pipeline-runs") {
        val runs = runCatching {
            PipelineRuns.userRuns(
                call.sessions.get<UserSession>()?.userId!!,
                call.request.queryParameters["code"] ?: ""
            )
        }.getOrElse { t ->
            call.application.environment.log.error("/api/actions", t)
            listOf()
        }
        call.respond(runs)
    }
}