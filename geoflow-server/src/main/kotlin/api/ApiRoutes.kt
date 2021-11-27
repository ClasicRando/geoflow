@file:Suppress("TooManyFunctions", "LongMethod")

package api

import auth.UserSession
import auth.requireUserRole
import database.Database
import database.enums.TaskStatus
import database.tables.Actions
import database.tables.InternalUsers
import database.tables.PipelineRunTasks
import database.tables.PipelineRuns
import database.tables.SourceTables
import database.tables.WorkflowOperations
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.routing.route
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.util.getOrFail
import jobs.SystemJob
import kjob
import kotlinx.coroutines.flow.toList
import mongo.MongoDb
import publisher

/** Base API route */
fun Route.api() {
    route("/api") {
        operations()
        actions()
        pipelineRuns()
        pipelineRunTasks()
        sourceTables()
        users()
        jobs()
    }
}

/** User operations API route */
private fun Route.operations() {
    route("/operations") {
        /** Returns list of operations based upon the current user's roles. */
        apiGet {
            val payload = Database.runWithConnection {
                WorkflowOperations.userOperations(it, call.sessions.get<UserSession>()?.roles!!)
            }
            ApiResponse.OperationsResponse(payload)
        }
    }
}

/** User actions API route */
private fun Route.actions() {
    route("/actions") {
        /** Returns list of actions based upon the current user's roles. */
        apiGet {
            val payload = Database.runWithConnection {
                Actions.userActions(it, call.sessions.get<UserSession>()?.roles!!)
            }
            ApiResponse.ActionsResponse(payload)
        }
    }
}

/** Pipeline runs API route */
private fun Route.pipelineRuns() {
    route("/pipeline-runs") {
        /** Returns list of pipeline runs for the given workflow code based upon the current user. */
        apiGet(path = "/{code}") {
            val payload = Database.runWithConnection {
                PipelineRuns.userRuns(
                    it,
                    call.sessions.get<UserSession>()?.userId!!,
                    call.parameters.getOrFail("code")
                )
            }
            ApiResponse.PipelineRunsResponse(payload)
        }
        apiPost(path = "/pickup/{runId}") {
            val runId = call.parameters.getOrFail("").toLong()
            Database.runWithConnection {
                PipelineRuns.pickupRun(it, runId, call.sessions.get<UserSession>()!!.userId)
            }
            ApiResponse.MessageResponse("Successfully picked up $runId to Active state under the current user")
        }
    }
}

/** Pipeline run tasks API route */
private fun Route.pipelineRunTasks() {
    route("/pipeline-run-tasks") {
        publisher(
            channelName = "pipelineRunTasks",
            listenId = "runId",
        ) { message ->
            Database.runWithConnection {
                PipelineRunTasks.getOrderedTasks(it, message.toLong())
            }
        }
        apiPost(path = "/reset-task/{prTaskId}") {
            val user = call.sessions.get<UserSession>()!!
            val pipelineRunTaskId = call.parameters.getOrFail("prTaskId").toLong()
            Database.runWithConnection {
                PipelineRunTasks.resetRecord(it, user.userId, pipelineRunTaskId)
            }
            ApiResponse.MessageResponse("Successfully reset task $pipelineRunTaskId")
        }
        apiPost(path = "/run-next/{runId}") {
            val user = call.sessions.get<UserSession>()!!
            val runId = call.parameters.getOrFail("runId").toLong()
            val payload = Database.runWithConnection {
                PipelineRunTasks.getRecordForRun(it, user.userId, runId)
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
            ApiResponse.NextTaskResponse(payload)
        }
        apiPost(path = "/run-all/{runId}") {
            val user = call.sessions.get<UserSession>()!!
            val runId = call.parameters.getOrFail("runId").toLong()
            val payload = Database.runWithConnection {
                PipelineRunTasks.getRecordForRun(it, user.userId, runId)
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
            ApiResponse.NextTaskResponse(payload)
        }
    }
}

/** Source tables API route */
private fun Route.sourceTables() {
    route("/source-tables") {
        apiGet(path = "/{runId}") {
            val runId = call.parameters.getOrFail("runId").toLong()
            val payload = Database.runWithConnection {
                SourceTables.getRunSourceTables(it, runId)
            }
            ApiResponse.SourceTablesResponse(payload)
        }
        apiPutReceive { sourceTable: SourceTables.Record ->
            val user = call.sessions.get<UserSession>()!!
            val payload = Database.useTransaction {
                SourceTables.updateSourceTable(it, user.userId, sourceTable)
            }
            ApiResponse.SourceTableResponse(payload)
        }
        apiPostReceive(path = "/{runId}") { sourceTable: SourceTables.Record ->
            val runId = call.parameters.getOrFail("runId").toLong()
            val user = call.sessions.get<UserSession>()!!
            val payload = Database.runWithConnection {
                SourceTables.insertSourceTable(it, runId, user.userId, sourceTable)
            }
            ApiResponse.InsertIdResponse(payload)
        }
        apiDelete(path = "/{stOid}") {
            val stOid = call.parameters.getOrFail("stOid").toLong()
            val user = call.sessions.get<UserSession>()!!
            Database.runWithConnection {
                SourceTables.deleteSourceTable(it, stOid, user.userId)
            }
            ApiResponse.MessageResponse("Deleted source table record ($stOid)")
        }
    }
}

/** Internal Users API route */
private fun Route.users() {
    route("/users") {
        apiGet {
            val user = call.requireUserRole("admin")
            val payload = Database.runWithConnection {
                InternalUsers.getUsers(it, user.userId)
            }
            ApiResponse.UsersResponse(payload)
        }
        apiPostReceive { requestUser: InternalUsers.RequestUser ->
            val userOid = Database.runWithConnection {
                InternalUsers.createUser(it, requestUser)
            }
            ApiResponse.InsertIdResponse(userOid)
        }
        apiPatchReceive { requestUser: InternalUsers.RequestUser ->
            val payload = Database.runWithConnection {
                InternalUsers.updateUser(it, requestUser)
            }
            ApiResponse.UserResponse(payload)
        }
    }
}

/** All KJob data API route */
private fun Route.jobs() {
    route("/jobs") {
        kjobTasks()
    }
}

/** KJob pipeline run tasks API route */
private fun Route.kjobTasks() {
    route("/tasks") {
        apiGet {
            val request = call.receive<MongoDb.TaskApiRequest>()
            val payload = MongoDb.getTasks(request).toList()
            ApiResponse.KJobTasksResponse(payload)
        }
    }
}
