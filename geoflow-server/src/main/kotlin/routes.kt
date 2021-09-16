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
        val runId = call.receiveParameters()["run_id"] ?: ""
        call.respondRedirect("/tasks?runId=$runId")
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
            PipelineRunTasks.getTasks(call.request.queryParameters["taskId"]?.toLong() ?: 0)
        }.getOrElse { t ->
            call.application.environment.log.error("/api/pipeline-run-tasks", t)
            listOf()
        }
        call.respond(tasks)
    }
    post("/api/run-task") {
        val runId = call.request.queryParameters["runId"]?.toLong() ?: 0
        val result = runCatching {
            PipelineRunTasks.getNextTask(runId)
        }
        if (result.isFailure) {
            call.application.environment.log.info("/api/run-task", result.exceptionOrNull()!!)
            call.respond(mapOf("error" to result.exceptionOrNull()!!.message))
        } else {
            val pipelineRunTask = result.getOrNull()!!
            if (pipelineRunTask.task.taskRunType == TaskRunType.User) {
                getUserPipelineTask(pipelineRunTask.pipelineRunTaskId, pipelineRunTask.task)
                    .runTask()
                call.respond(mapOf("success" to "Completed ${pipelineRunTask.pipelineRunTaskId}"))
            } else {
                pipelineRunTask.taskStatus = TaskStatus.Scheduled
                pipelineRunTask.flushChanges()
                kjob.schedule(SystemJob) {
                    props[it.pipelineRunTaskId] = pipelineRunTask.pipelineRunTaskId
                    props[it.taskClassName] = pipelineRunTask.task.taskClassName
                }
                call.respond(mapOf("success" to "Scheduled ${pipelineRunTask.pipelineRunTaskId}"))
            }
        }
    }
}