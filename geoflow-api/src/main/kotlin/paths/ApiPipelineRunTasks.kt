package paths

import utils.apiCall
import database.Database
import database.enums.TaskStatus
import database.tables.PipelineRunTasks
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import jobs.SystemJob
import sockets.publisher
import scheduleJob
import utils.ApiResponse

/** Pipeline run tasks API route */
object ApiPipelineRunTasks : ApiPath(path = "/pipeline-run-tasks") {

    override fun Route.registerEndpoints() {
        getTasks(this)
        resetTask(this)
        runNext(this)
        runAll(this)
    }

    /** Publisher method using a websocket to provided updates to the user about the provided table and listenId */
    private fun getTasks(parent: Route) {
        parent.publisher(channelName = "pipelineRunTasks", listenId = "runId") { message ->
            Database.runWithConnection {
                PipelineRunTasks.getOrderedTasks(it, message.toLong())
            }
        }
    }

    /**
     * Requests that the specified task is reset for rerun. All child tasks will be removed. Returns a message
     * response indicating if the specified task was successfully reset
     */
    private fun resetTask(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Post, path = "/reset-task/{prTaskId}") { userOid ->
            val pipelineRunTaskId = call.parameters.getOrFail("prTaskId").toLong()
            Database.runWithConnection {
                PipelineRunTasks.resetRecord(it, userOid, pipelineRunTaskId)
            }
            ApiResponse.MessageResponse("Successfully reset task $pipelineRunTaskId")
        }
    }

    /**
     * Requests to schedule the next available task for the provided run. Returns data about the NextTask scheduled
     * to run
     */
    private fun runNext(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Post, path = "/run-next/{runId}") { userOid ->
            val runId = call.parameters.getOrFail("runId").toLong()
            val payload = Database.runWithConnection {
                PipelineRunTasks.getRecordForRun(it, userOid, runId)
            }.also { nextTask ->
                scheduleJob(SystemJob) {
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
    }

    /**
     * Requests to schedule the next available task for the provided run. Includes request to continue running the
     * following tasks if the previous task execution was successful and the next task is a System Task. Returns
     * data about the NextTask schedule to run
     */
    private fun runAll(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Post, path = "/run-all/{runId}") { userOid ->
            val runId = call.parameters.getOrFail("runId").toLong()
            val payload = Database.runWithConnection {
                PipelineRunTasks.getRecordForRun(it, userOid, runId)
            }.also { nextTask ->
                scheduleJob(SystemJob) {
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
