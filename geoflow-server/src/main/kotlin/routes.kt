import auth.UserSession
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
            pipelineStatus("collection")
        }
    }
    get("/load") {
        call.respondHtml {
            pipelineStatus("load")
        }
    }
    get("/check") {
        call.respondHtml {
            pipelineStatus("check")
        }
    }
    get("/qa") {
        call.respondHtml {
            pipelineStatus("qa")
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