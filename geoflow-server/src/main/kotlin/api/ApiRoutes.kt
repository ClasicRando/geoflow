@file:Suppress("TooManyFunctions")

package api

import auth.UserSession
import database.Database
import database.enums.TaskStatus
import database.tables.Actions
import database.tables.PipelineRunTasks
import database.tables.PipelineRuns
import database.tables.SourceTables
import database.tables.WorkflowOperations
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.util.getOrFail
import io.ktor.websocket.webSocket
import jobs.SystemJob
import kjob
import publisher2

/** Base API route */
fun Route.api() {
    route("/api/v2") {
        operations()
        actions()
        pipelineRuns()
        pipelineRunTasks()
        sourceTables()
    }
}

/** User operations API route */
private fun Route.operations() {
    route("/operations") {
        /** Returns list of operations based upon the current user's roles. */
        get {
            apiResponseMulti(
                responseObject = "operation",
                errorMessage = "Failed to get user operations",
            ) {
                Database.runWithConnection {
                    WorkflowOperations.userOperations(it, call.sessions.get<UserSession>()?.roles!!)
                }
            }
        }
    }
}

/** User actions API route */
private fun Route.actions() {
    route("/actions") {
        /** Returns list of actions based upon the current user's roles. */
        get {
            apiResponseMulti(
                responseObject = "actions",
                errorMessage = "Failed to get user actions",
            ) {
                Database.runWithConnection {
                    Actions.userActions(it, call.sessions.get<UserSession>()?.roles!!)
                }
            }
        }
    }
}

/** Pipeline runs API route */
private fun Route.pipelineRuns() {
    route("/pipeline-runs") {
        /** Returns list of pipeline runs for the given workflow code based upon the current user. */
        get("/{code}") {
            apiResponseMulti(
                responseObject = "pipeline_run",
                errorMessage = "Failed to get user pipeline runs",
            ) {
                Database.runWithConnection {
                    PipelineRuns.userRuns(
                        it,
                        call.sessions.get<UserSession>()?.userId!!,
                        call.parameters.getOrFail("code")
                    )
                }
            }
        }
        patch("/pickup/{runId}") {
            apiResponseSingle(
                responseObject = "string",
                errorMessage = "Failed to pickup pipeline run",
            ) {
                val runId = call.parameters.getOrFail("").toLong()
                Database.runWithConnection {
                    PipelineRuns.pickupRun(it, runId, call.sessions.get<UserSession>()!!.userId)
                }
                "Successfully picked up $runId to Active state under the current user"
            }
        }
    }
}

/** Pipeline run tasks API route */
private fun Route.pipelineRunTasks() {
    route("/pipeline-run-tasks") {
        publisher2(
            channelName = "pipelineRunTasks",
            listenId = "runId",
        ) { message ->
            Database.runWithConnection {
                PipelineRunTasks.getOrderedTasks(it, message.toLong())
            }
        }
        post("/run-next/{runId}") {
            apiResponseSingle(
                responseObject = "next_task",
                errorMessage = "Failed to run next task",
            ) {
                val user = call.sessions.get<UserSession>()!!
                val runId = call.parameters.getOrFail("runId").toLong()
                Database.runWithConnection {
                    PipelineRunTasks.getRecordForRun(it, user.username, runId)
                }.also { nextTask ->
                    kjob.schedule(SystemJob) {
                        props[it.pipelineRunTaskId] = nextTask.pipelineRunTaskId
                        props[it.runId] = runId
                        props[it.runNext] = false
                    }
                    Database.runWithConnection {
                        PipelineRunTasks.setStatus(it, nextTask.pipelineRunTaskId, TaskStatus.Scheduled)
                    }
                }
            }
        }
        post("/run-all/{runId}") {
            apiResponseSingle(
                responseObject = "next_task",
                errorMessage = "Failed to run all available tasks",
            ) {
                val user = call.sessions.get<UserSession>()!!
                val runId = call.parameters.getOrFail("runId").toLong()
                Database.runWithConnection {
                    PipelineRunTasks.getRecordForRun(it, user.username, runId)
                }.also { nextTask ->
                    kjob.schedule(SystemJob) {
                        props[it.pipelineRunTaskId] = nextTask.pipelineRunTaskId
                        props[it.runId] = runId
                        props[it.runNext] = true
                    }
                    Database.runWithConnection {
                        PipelineRunTasks.setStatus(it, nextTask.pipelineRunTaskId, TaskStatus.Scheduled)
                    }
                }
            }
        }
    }
}

/** Source tables API route */
private fun Route.sourceTables() {
    route("/source-tables") {
        get("/{runId}") {
            apiResponseMulti(
                responseObject = "source_table",
                errorMessage = "Failed to get source tables",
            ) {
                val runId = call.parameters.getOrFail("runId").toLong()
                Database.runWithConnection {
                    SourceTables.getRunSourceTables(it, runId)
                }
            }
        }
        put {
            apiResponseSingle(
                responseObject = "source_table",
                errorMessage = "Failed to update source table record",
            ) {
                val requestBody = call.receive<SourceTables.Record>()
                val user = call.sessions.get<UserSession>()!!
                Database.runWithConnection {
                    SourceTables.updateSourceTableV2(it, user.username, requestBody)
                }
            }
        }
    }
}
