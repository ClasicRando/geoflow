import auth.UserSession
import html.*
import io.ktor.application.*
import io.ktor.html.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import jobs.SystemJob
import orm.enums.TaskRunType
import orm.enums.TaskStatus
import orm.tables.Actions
import orm.tables.PipelineRunTasks
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
    get("/pipeline-status") {
        val code = call.request.queryParameters["code"] ?: ""
        call.respondHtml {
            if (call.sessions.get<UserSession>()!!.hasRole(code)) {
                pipelineStatus(code)
            } else {
                accessRestricted(code)
            }
        }
    }
    post("/pipeline-status") {
        val params = call.receiveParameters()
        val pickup = params["pickup"] ?: ""
        val runId = params["run_id"] ?: ""
        val redirect = if (pickup.isEmpty()) {
            "/tasks?runId=$runId"
        } else {
            runCatching {
                PipelineRuns.pickupRun(
                    runId.toLong(),
                    call.sessions.get<UserSession>()!!.userId
                )
                "/tasks?runId=$runId"
            }.getOrElse { t ->
                call.application.environment.log.error("/pipeline-stats: pickup", t)
                "/invalid-parameter?message=Could+not+pickup+run"
            }
        }
        call.respondRedirect(redirect)
    }
}

fun Route.pipelineTasks() {
    get("/tasks") {
        val runIdStr = call.request.queryParameters["runId"] ?: "0"
        val runId = runIdStr.toLong()
        val run = PipelineRuns.getRun(runId)
        val runUsernames = listOf(
            run?.collectionUser?.username,
            run?.loadUser?.username,
            run?.checkUser?.username,
            run?.qaUser?.username,
        ).mapNotNull { it }
        val user = call.sessions.get<UserSession>()!!
        when {
            run == null -> call.respondHtml { invalidParameter("Run ID provided cannot be found") }
            user.hasRole("admin") || user.username in runUsernames -> {
                call.respondHtml { pipelineTasks(runId) }
            }
            else -> call.respondHtml { invalidParameter("You must be a part of this run to view it") }
        }
    }
}

fun Route.invalidParameter() {
    get("/invalid-parameter") {
        call.respondHtml {
            invalidParameter(call.request.queryParameters["message"] ?: "")
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
    get("/api/pipeline-runs") {
        val runs = runCatching {
            PipelineRuns.userRuns(
                call.sessions.get<UserSession>()?.userId!!,
                call.request.queryParameters["code"] ?: ""
            )
        }.getOrElse { t ->
            call.application.environment.log.error("/api/pipeline-runs", t)
            listOf()
        }
        call.respond(runs)
    }
    get("/api/pipeline-run-tasks") {
        val tasks = runCatching {
            PipelineRunTasks.getOrderedTasks(call.request.queryParameters["taskId"]?.toLong() ?: 0)
        }.getOrElse { t ->
            call.application.environment.log.error("/api/pipeline-run-tasks", t)
            listOf()
        }
        call.respond(tasks)
    }
    post("/api/run-task") {
        val runId = call.request.queryParameters["runId"]?.toLong() ?: 0
        val response = runCatching {
            PipelineRunTasks.getNextTask(runId)?.let { pipelineRunTask ->
                if (pipelineRunTask.taskRunType == TaskRunType.User) {
                    getUserPipelineTask(pipelineRunTask.pipelineRunTaskId, pipelineRunTask.taskClassName)
                        .runTask()
                    mapOf("success" to "Completed ${pipelineRunTask.pipelineRunTaskId}")
                } else {
                    PipelineRunTasks.setStatus(pipelineRunTask.pipelineRunTaskId, TaskStatus.Scheduled)
                    kjob.schedule(SystemJob) {
                        props[it.pipelineRunTaskId] = pipelineRunTask.pipelineRunTaskId
                        props[it.taskClassName] = pipelineRunTask.taskClassName
                    }
                    mapOf("success" to "Scheduled ${pipelineRunTask.pipelineRunTaskId}")
                }
            } ?: mapOf("error" to "Could not get next task")
        }.getOrElse { t ->
            call.application.environment.log.info("/api/run-task", t)
            mapOf("error" to t.message)
        }
        call.respond(response)
    }
}