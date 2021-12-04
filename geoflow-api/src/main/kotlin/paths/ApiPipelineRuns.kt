package paths

import apiCall
import database.Database
import database.tables.PipelineRuns
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.routing.Route
import io.ktor.util.getOrFail

/** Pipeline runs API route */
object ApiPipelineRuns : ApiPath(path = "/pipeline-runs")  {

    override fun Route.registerEndpoints() {
        getRuns(this)
        pickupRun(this)
    }

    /** Returns list of pipeline runs for the given workflow code based upon the current user. */
    private fun getRuns(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Get, path = "/{code}") { userOid ->
            val payload = Database.runWithConnection {
                PipelineRuns.userRuns(it, userOid, call.parameters.getOrFail("code"))
            }
            ApiResponse.PipelineRunsResponse(payload)
        }
    }

    /**
     * Requests that the specified run to be set to the user who made the API request. Returns a message indicating
     * if the provided runId was successfully picked up
     */
    private fun pickupRun(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Post, path = "/pickup/{runId}") { userOid ->
            val runId = call.parameters.getOrFail("").toLong()
            Database.runWithConnection {
                PipelineRuns.pickupRun(it, runId, userOid)
            }
            ApiResponse.MessageResponse("Successfully picked up $runId to Active state under the current user")
        }
    }

}
