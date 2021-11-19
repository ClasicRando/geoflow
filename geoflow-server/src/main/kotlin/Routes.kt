import auth.UserSession
import database.Database
import database.enums.TaskStatus
import database.tables.PipelineRunTasks
import database.tables.PipelineRuns
import database.tables.SourceTables
import database.tables.InternalUsers
import database.tables.Actions
import database.tables.WorkflowOperations
import html.createUser
import html.index
import html.pipelineStatus
import html.pipelineTasks
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.content.static
import io.ktor.http.content.resources
import io.ktor.request.uri
import io.ktor.request.receiveParameters
import io.ktor.response.respondRedirect
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.patch
import io.ktor.routing.delete
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.util.getOrFail
import io.ktor.util.toMap
import jobs.SystemJob

/** Entry route that handles empty an empty path or the index route. Empty routes are redirected to index. */
fun Route.index() {
    get("/") {
        call.respondRedirect("/index")
    }
    get("/index") {
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
fun Route.pipelineStatus() = route("/pipeline-status/{code}") {
    get {
        val code = call.parameters.getOrFail("code")
        require(call.sessions.get<UserSession>()!!.hasRole(code)) {
            UnauthorizedRouteAccessException(call.request.uri)
        }
        call.respondHtml {
            pipelineStatus(code)
        }
    }
    post {
        val params = call.receiveParameters()
        val runId = params.getOrFail("run_id").toLong()
        try {
            Database.runWithConnection {
                PipelineRuns.pickupRun(it, runId, call.sessions.get<UserSession>()!!.userId)
            }
        } catch (t: Throwable) {
            call.application.environment.log.error("/pipeline-stats: pickup", t)
            throw t
        }
        call.respondRedirect("/tasks/$runId")
    }
}

/** Pipeline tasks route that handles request to view a specific run's task list. */
fun Route.pipelineTasks() = route("/tasks/{runId}") {
    get {
        call.respondHtml {
            pipelineTasks(call.parameters.getOrFail("runId").toLong())
        }
    }
}

fun Route.users() = route("/users") {
    route("/create") {
        get {
            require(call.sessions.get<UserSession>()!!.hasRole("admin")) {
                UnauthorizedRouteAccessException(call.request.uri)
            }
            call.respondHtml {
                createUser()
            }
        }
        post {
            require(call.sessions.get<UserSession>()!!.hasRole("admin")) {
                UnauthorizedRouteAccessException(call.request.uri)
            }
            val parameters = call.receiveParameters().toMap()
            Database.runWithConnection {
                InternalUsers.createUser(it, parameters)
            }?.let { userOid ->
                call.application.environment.log.info("Created new user $userOid")
            }
            call.respondRedirect("/index")
        }
    }
}

/** Base API route. This will be moved to a new module/service later in development */
fun Route.api() = route("/api") {
    /** Returns list of operations based upon the current user's roles. */
    get("/operations") {
        val operations = runCatching {
            Database.runWithConnection {
                WorkflowOperations.userOperations(it, call.sessions.get<UserSession>()?.roles!!)
            }
        }.getOrElse { t ->
            call.application.environment.log.error("/api/operations", t)
            listOf()
        }
        call.respond(operations)
    }
    /** Returns list of actions based upon the current user's roles. */
    get("/actions") {
        val actions = runCatching {
            Database.runWithConnection {
                Actions.userActions(it, call.sessions.get<UserSession>()?.roles!!)
            }
        }.getOrElse { t ->
            call.application.environment.log.error("/api/actions", t)
            listOf()
        }
        call.respond(actions)
    }
    /** Returns list of pipeline runs for the given workflow code based upon the current user. */
    get("/pipeline-runs/{code}") {
        val runs = runCatching {
            Database.runWithConnection {
                PipelineRuns.userRuns(
                    it,
                    call.sessions.get<UserSession>()?.userId!!,
                    call.parameters.getOrFail("code")
                )
            }
        }.getOrElse { t ->
            call.application.environment.log.error("/api/pipeline-runs", t)
            listOf()
        }
        call.respond(runs)
    }
    /** Returns list of pipeline tasks for the given run. */
    get("/pipeline-run-tasks/{runId}") {
        val tasks = runCatching {
            Database.runWithConnection {
                PipelineRunTasks.getOrderedTasks(it, call.parameters.getOrFail("runId").toLong())
            }
        }.getOrElse { t ->
            call.application.environment.log.error("/api/pipeline-run-tasks", t)
            listOf()
        }
        call.respond(tasks)
    }
    /**
     * Tries to run a specified pipeline task based upon the pipeline run task ID provided.
     *
     * If the underlining task is a User task then it is run right away and the response is a completed message. If the
     * underlining task is a System task then it is scheduled to run and the response is a scheduled message. During the
     * task fetching and assessment, an error can be thrown. The error is caught, logged and the response becomes an
     * error message.
     */
    post("/run-task/{runId}") {
        val user = call.sessions.get<UserSession>()!!
        val runId = call.parameters.getOrFail("runId").toLong()
        val response = runCatching {
            val pipelineRunTask = Database.runWithConnection {
                PipelineRunTasks.getRecordForRun(it, user.username, runId)
            }
            Database.runWithConnection {
                PipelineRunTasks.setStatus(it, pipelineRunTask.pipelineRunTaskId, TaskStatus.Scheduled)
            }
            kjob.schedule(SystemJob) {
                props[it.pipelineRunTaskId] = pipelineRunTask.pipelineRunTaskId
                props[it.runId] = runId
                props[it.taskClassName] = pipelineRunTask.taskClassName
                props[it.runNext] = false
            }
            mapOf("success" to "Scheduled ${pipelineRunTask.pipelineRunTaskId}")
        }.getOrElse { t ->
            call.application.environment.log.info("/api/run-task", t)
            mapOf("error" to t.message)
        }
        call.respond(response)
    }
    /**
     * Tries to run a specified pipeline task based upon the pipeline run task ID provided while continuing to run
     * subsequent tasks until a user task appears or the current running task fail.
     *
     * If the underlining task is a User task then it is run right away and the response is a completed message. If the
     * underlining task is a System task then it is scheduled to run and the response is a scheduled message. The prop
     * of 'runNext' is also set to true to tell the worker to continue running tasks after successful completion.
     * During the task fetching and assessment, an error can be thrown. The error is caught, logged and the response
     * becomes an error message.
     */
    post("/run-all/{runId}") {
        val user = call.sessions.get<UserSession>()!!
        val runId = call.parameters.getOrFail("runId").toLong()
        val response = runCatching {
            val pipelineRunTask = Database.runWithConnection {
                PipelineRunTasks.getRecordForRun(it, user.username, runId)
            }
            Database.runWithConnection {
                PipelineRunTasks.setStatus(it, pipelineRunTask.pipelineRunTaskId, TaskStatus.Scheduled)
            }
            kjob.schedule(SystemJob) {
                props[it.pipelineRunTaskId] = pipelineRunTask.pipelineRunTaskId
                props[it.runId] = runId
                props[it.taskClassName] = pipelineRunTask.taskClassName
                props[it.runNext] = true
            }
            mapOf("success" to "Scheduled ${pipelineRunTask.pipelineRunTaskId}")
        }.getOrElse { t ->
            call.application.environment.log.info("/api/run-all", t)
            mapOf("error" to t.message)
        }
        call.respond(response)
    }
    /** Resets the provided pipeline run task to a waiting state and deletes all child tasks if any exist. */
    post("/reset-task/{runId}/{prTaskId}") {
        val user = call.sessions.get<UserSession>()!!
        val runId = call.parameters.getOrFail("runId").toLong()
        val pipelineRunTaskId = call.parameters.getOrFail("prTaskId").toLong()
        val response = runCatching {
            Database.runWithConnection {
                PipelineRunTasks.resetRecord(it, user.username, runId, pipelineRunTaskId)
            }
            mapOf("success" to "Reset $pipelineRunTaskId")
        }.getOrElse { t ->
            call.application.environment.log.info("/api/reset-task", t)
            mapOf("error" to t.message)
        }
        call.respond(response)
    }
    /** Source tables route */
    route("/source-tables") {
        /** Returns all source table records for the provided pipeline run */
        get("/{runId}") {
            val runId = call.parameters.getOrFail("runId").toLong()
            val response = runCatching {
                Database.runWithConnection {
                    SourceTables.getRunSourceTables(it, runId)
                }
            }.getOrElse { t ->
                call.application.environment.log.info("/api/source-tables", t)
                listOf()
            }
            call.respond(response)
        }
        /** Updates a source table record (specified by the stOid) with the provided parameters */
        patch {
            val user = call.sessions.get<UserSession>()!!
            val params = call.request.queryParameters.names().associateWith { call.request.queryParameters[it] }
            val response = runCatching {
                val (stOid, updateCount) = Database.runWithConnection {
                    SourceTables.updateSourceTable(it, user.username, params)
                }
                mapOf("success" to "updated stOid = $stOid. Records affected, $updateCount")
            }.getOrElse { t ->
                call.application.environment.log.info("/api/source-tables", t)
                val message = "Failed to update stOid ${params["stOid"]}"
                mapOf("error" to "$message. ${t.message}")
            }
            call.respond(response)
        }
        /** Creates a new source table record with the provided parameters */
        post {
            val user = call.sessions.get<UserSession>()!!
            val params = call.request.queryParameters.names().associateWith { call.request.queryParameters[it] }
            val response = runCatching {
                val stOid = Database.runWithConnection {
                    SourceTables.insertSourceTable(it, user.username, params)
                }
                mapOf("success" to "inserted stOid $stOid")
            }.getOrElse { t ->
                call.application.environment.log.info("/api/source-tables", t)
                val message = "Failed to insert new source table"
                mapOf("error" to "$message. ${t.message}")
            }
            call.respond(response)
        }
        /** Deletes an existing source table record with the provided stOid */
        delete {
            val user = call.sessions.get<UserSession>()!!
            val params = call.request.queryParameters.names().associateWith { call.request.queryParameters[it] }
            val response = runCatching {
                val stOid = Database.runWithConnection {
                    SourceTables.deleteSourceTable(it, user.username, params)
                }
                mapOf("success" to "deleted stOid $stOid")
            }.getOrElse { t ->
                call.application.environment.log.info("/api/source-tables", t)
                val message = "Failed to delete stOid ${params["stOid"]}"
                mapOf("error" to "$message. ${t.message}")
            }
            call.respond(response)
        }
    }
}

/** WebSocket routes used in pub/sub pattern. */
fun Route.sockets() = route("/sockets") {
    /**  */
    publisher("/pipeline-run-tasks", "pipelineRunTasks")
}

/** Route for static Javascript assets */
fun Route.js() {
    static("assets") {
        resources("javascript")
    }
}
