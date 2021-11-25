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
import io.ktor.routing.delete
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.util.getOrFail
import jobs.SystemJob
import kjob
import kotlinx.coroutines.flow.toList
import mongo.MongoDb
import publisher2

/** Base API route */
fun Route.apiv2() {
    route("/api/v2") {
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
        apiGet("/{code}") {
            val payload = Database.runWithConnection {
                PipelineRuns.userRuns(
                    it,
                    call.sessions.get<UserSession>()?.userId!!,
                    call.parameters.getOrFail("code")
                )
            }
            ApiResponse.PipelineRunsResponse(payload)
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
        apiGet("/{runId}") {
            val runId = call.parameters.getOrFail("runId").toLong()
            val payload = Database.runWithConnection {
                SourceTables.getRunSourceTables(it, runId)
            }
            ApiResponse.SourceTablesResponse(payload)
        }
        put {
            apiResponseSingle(
                responseObject = "source_table",
                errorMessage = "Failed to update source table record",
            ) {
                val requestBody = call.receive<SourceTables.Record>()
                val user = call.sessions.get<UserSession>()!!
                Database.useTransaction {
                    SourceTables.updateSourceTableV2(it, user.username, requestBody)
                }
            }
        }
        post("/{runId}")  {
            apiResponseSingle(
                responseObject = "source_table",
                errorMessage = "Failed to insert source table record",
            ) {
                val runId = call.parameters.getOrFail("runId").toLong()
                val requestBody = call.receive<SourceTables.Record>()
                val user = call.sessions.get<UserSession>()!!
                Database.runWithConnection {
                    SourceTables.insertSourceTableV2(it, runId, user.username, requestBody)
                }
            }
        }
        delete("/{runId}/{stOid}") {
            apiResponseSingle(
                responseObject = "source_table",
                errorMessage = "Failed to insert source table record",
            ) {
                val runId = call.parameters.getOrFail("runId").toLong()
                val stOid = call.parameters.getOrFail("stOid").toLong()
                val user = call.sessions.get<UserSession>()!!
                Database.runWithConnection {
                    SourceTables.deleteSourceTableV2(it, stOid, runId, user.username)
                }
            }
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
        post {
            apiResponseSingle(
                responseObject = "source_table",
                errorMessage = "Failed to insert new user",
            ) {
                val user = call.receive<InternalUsers.RequestUser>()
                val userOid = Database.runWithConnection { InternalUsers.createUser(it, user) }
                    ?: error("INSERT statement did not return any data")
                "Created new user, ${user.username} (${userOid})"
            }
        }
        patch {
            apiResponseSingle(
                responseObject = "source_table",
                errorMessage = "Failed to update user",
            ) {
                val user = call.receive<InternalUsers.RequestUser>()
                Database.runWithConnection {
                    InternalUsers.updateUserV2(it, user)
                }
            }
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
