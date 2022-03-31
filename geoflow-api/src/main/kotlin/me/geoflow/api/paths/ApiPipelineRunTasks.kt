package me.geoflow.api.paths

import me.geoflow.core.database.tables.PipelineRunTasks
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.core.jobs.SystemJob
import me.geoflow.api.scheduleJob
import me.geoflow.api.sockets.postgresPublisher
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
import me.geoflow.core.database.enums.TaskStatus

/** Pipeline run tasks API route */
@Suppress("unused")
object ApiPipelineRunTasks : ApiPath(path = "/pipeline-run-tasks") {

    override fun Route.registerEndpoints() {
        getTasks(this)
        resetTask(this)
        runNext(this)
        runAll(this)
    }

    /** Publisher method using a websocket to provided updates to the user about the provided table and listenId */
    private fun getTasks(parent: Route) {
        parent.postgresPublisher(channelName = "pipelineRunTasks", listenId = "runId") { message, connection ->
            PipelineRunTasks.getOrderedTasksNew(connection, message.toLong())
        }
    }

    /**
     * Requests that the specified task is reset for rerun. All child tasks will be removed. Returns a message
     * response indicating if the specified task was successfully reset
     */
    private fun resetTask(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Post, path = "/reset-task/{prTaskId}") { userOid, connection ->
            val pipelineRunTaskId = call.parameters.getOrFail<Long>("prTaskId")
            PipelineRunTasks.resetRecord(connection, userOid, pipelineRunTaskId)
            ApiResponse.MessageResponse("Successfully reset task $pipelineRunTaskId")
        }
    }

    /**
     * Requests to schedule the next available task for the provided run. Returns data about the NextTask scheduled
     * to run
     */
    private fun runNext(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Post, path = "/run-next/{runId}") { userOid, connection ->
            val runId = call.parameters.getOrFail<Long>("runId")
            val nextTask = PipelineRunTasks.getRecordForRun(connection, userOid, runId)
            scheduleJob(SystemJob) {
                props[it.pipelineRunTaskId] = nextTask.pipelineRunTaskId
                props[it.runId] = runId
                props[it.runNext] = false
            }
            PipelineRunTasks.setStatus(connection, nextTask.pipelineRunTaskId, TaskStatus.Scheduled)
            ApiResponse.NextTaskResponse(nextTask)
        }
    }

    /**
     * Requests to schedule the next available task for the provided run. Includes request to continue running the
     * following tasks if the previous task execution was successful and the next task is a System Task. Returns
     * data about the NextTask schedule to run
     */
    private fun runAll(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Post, path = "/run-all/{runId}") { userOid, connection ->
            val runId = call.parameters.getOrFail<Long>("runId")
            val nextTask = PipelineRunTasks.getRecordForRun(connection, userOid, runId)
            scheduleJob(SystemJob) {
                props[it.pipelineRunTaskId] = nextTask.pipelineRunTaskId
                props[it.runId] = runId
                props[it.runNext] = true
            }
            PipelineRunTasks.setStatus(connection, nextTask.pipelineRunTaskId, TaskStatus.Scheduled)
            ApiResponse.NextTaskResponse(nextTask)
        }
    }
}
