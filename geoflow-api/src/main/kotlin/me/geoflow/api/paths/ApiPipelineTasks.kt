package me.geoflow.api.paths

import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
import me.geoflow.core.database.tables.PipelineTasks
import me.geoflow.core.database.tables.records.PipelineTask

/** Provs API route */
@Suppress("unused")
object ApiPipelineTasks : ApiPath(path = "/pipeline-tasks") {

    override fun Route.registerEndpoints() {
        getPipelineTasks(this)
    }

    /** Returns a list of all pipeline tasks for a given pipelineId */
    private fun getPipelineTasks(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/{pipelineId}") { _, connection ->
            val payload = PipelineTasks.getRecords(connection, call.parameters.getOrFail<Long>("pipelineId"))
            ApiResponse.PipelineTasksResponse(payload)
        }
    }

    /**
     * Attempts to set the pipeline tasks for a given pipelineId using the body of the request.
     *
     * Uses a transaction to delete all pipeline tasks for the pipelineId then attempts to insert the list of objects
     * in the request body. If the insert fails, the changes are rolled back to preserve the current state of the
     * pipelineId
     */
    private fun setPipelineTasks(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/{pipelineId}") { userId, connection ->
            val pipelineId = call.parameters.getOrFail<Long>("pipelineId")
            val pipelineTasks = call.receive<List<PipelineTask>>()
            val (delete, insert) = PipelineTasks.setRecords(connection, userId, pipelineId, pipelineTasks)
            val payload = "Successful set! Deleted $delete record(s), inserted $insert record(s)"
            ApiResponse.MessageResponse(payload)
        }
    }

}
