package me.geoflow.api.paths

import me.geoflow.api.utils.apiCall
import me.geoflow.core.database.tables.WorkflowOperations
import io.ktor.http.HttpMethod
import io.ktor.routing.Route
import me.geoflow.api.utils.ApiResponse
import me.geoflow.core.database.Database

/** User operations API route */
object ApiOperations : ApiPath(path = "/operations") {

    override fun Route.registerEndpoints() {
        getOperations(this)
    }

    /** Returns list of operations based upon the current user's roles. */
    private fun getOperations(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Get) { userOid ->
            val payload = Database.runWithConnection {
                WorkflowOperations.userOperations(it, userOid)
            }
            ApiResponse.OperationsResponse(payload)
        }
    }

}
