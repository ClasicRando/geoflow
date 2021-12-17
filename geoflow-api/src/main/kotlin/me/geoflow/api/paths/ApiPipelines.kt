package me.geoflow.api.paths

import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
import me.geoflow.core.database.tables.Pipelines
import me.geoflow.core.database.tables.records.Pipeline

/** Pipelines API route */
@Suppress("unused")
object ApiPipelines : ApiPath(path = "/pipelines") {

    override fun Route.registerEndpoints() {
        getPipeline(this)
        getPipelines(this)
        createPipeline(this)
        updatePipeline(this)
    }

    /** Returns a list of all pipelines available */
    private fun getPipelines(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get) { _, connection ->
            val payload = Pipelines.getRecords(connection)
            ApiResponse.PipelinesResponse(payload)
        }
    }

    /** Returns a single pipeline record */
    private fun getPipeline(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/{pipelineId}") { _, connection ->
            val payload = Pipelines.getRecord(connection, call.parameters.getOrFail<Long>("pipelineId"))
            ApiResponse.PipelineResponse(payload)
        }
    }

    /** Attempts to create a new pipeline record with the provided request body */
    private fun createPipeline(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get) { userId, connection ->
            val pipeline = call.receive<Pipeline>()
            val payload = Pipelines.createRecord(connection, userId, pipeline)
            ApiResponse.InsertIdResponse(payload)
        }
    }

    /** Attempts to update an existing pipeline record with a new name. Will not update the workflow operation field */
    private fun updatePipeline(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get) { userId, connection ->
            val pipeline = call.receive<Pipeline>()
            Pipelines.updateName(connection, userId, pipeline)
            ApiResponse.PipelineResponse(pipeline)
        }
    }

}
