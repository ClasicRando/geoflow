package me.geoflow.api.paths

import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
import me.geoflow.core.database.tables.PipelineRelationships

/** */
@Suppress("unused")
object ApiPipelineRelationships : ApiPath("/pipeline-relationships") {

    override fun Route.registerEndpoints() {
        getRunRelationships(this)
    }

    /** */
    private fun getRunRelationships(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/{runId}") { _, connection ->
            val payload = PipelineRelationships.getRecords(connection, call.parameters.getOrFail<Long>("runId"))
            ApiResponse.PipelineRelationshipsResponse(payload)
        }
    }

}
