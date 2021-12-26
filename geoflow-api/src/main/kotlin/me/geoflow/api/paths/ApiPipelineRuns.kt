package me.geoflow.api.paths

import me.geoflow.core.database.tables.PipelineRuns
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres

/** Pipeline runs API route */
@Suppress("unused")
object ApiPipelineRuns : ApiPath(path = "/pipeline-runs")  {

    override fun Route.registerEndpoints() {
        getRuns(this)
        pickupRun(this)
        moveForwardRun(this)
        moveBackRun(this)
    }

    /** Returns list of pipeline runs for the given workflow code based upon the current user. */
    private fun getRuns(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/{code}") { userOid, connection ->
            val payload = PipelineRuns.userRuns(connection, userOid, call.parameters.getOrFail("code"))
            ApiResponse.PipelineRunsResponse(payload)
        }
    }

    /**
     * Requests that the specified run to be set to the user who made the API request. Returns a message indicating
     * if the provided runId was successfully picked up
     */
    private fun pickupRun(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Post, path = "/pickup/{runId}") { userOid, connection ->
            val runId = call.parameters.getOrFail<Long>("runId")
            PipelineRuns.pickupRun(connection, runId, userOid)
            ApiResponse.MessageResponse("Successfully picked up $runId to Active state under the current user")
        }
    }

    /**
     * Requests that the specified run to be moved to the next workflow state. Returns a message indicating if the
     * provided runId was successfully moved forward
     */
    private fun moveForwardRun(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Post, path = "/move-forward/{runId}") { userOid, connection ->
            val runId = call.parameters.getOrFail<Long>("runId")
            PipelineRuns.moveForwardRun(connection, runId, userOid)
            ApiResponse.MessageResponse("Successfully moved $runId to the next workflow state")
        }
    }

    /**
     * Requests that the specified run to be moved to the previous workflow state or operation. Returns a message
     * indicating if the provided runId was successfully moved back
     */
    private fun moveBackRun(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Post, path = "/move-back/{runId}") { userOid, connection ->
            val runId = call.parameters.getOrFail<Long>("runId")
            PipelineRuns.moveBackRun(connection, runId, userOid)
            ApiResponse.MessageResponse("Successfully moved $runId to the next workflow state")
        }
    }

}
