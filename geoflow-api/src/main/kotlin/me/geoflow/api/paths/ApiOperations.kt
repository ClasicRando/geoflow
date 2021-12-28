package me.geoflow.api.paths

import me.geoflow.core.database.tables.WorkflowOperations
import io.ktor.http.HttpMethod
import io.ktor.routing.Route
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres

/** User operations API route */
@Suppress("unused")
object ApiOperations : ApiPath(path = "/operations") {

    override fun Route.registerEndpoints() {
        getOperations(this)
        getDataOperations(this)
    }

    /** Returns list of operations based upon the current user's roles. */
    private fun getOperations(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get) { userOid, connection ->
            val payload = WorkflowOperations.userOperations(connection, userOid)
            ApiResponse.OperationsResponse(payload)
        }
    }

    /** Returns list of data operations based upon the current user's roles. */
    private fun getDataOperations(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/data") { userOid, connection ->
            val payload = WorkflowOperations.dataOperations(connection, userOid)
            ApiResponse.OperationsResponse(payload)
        }
    }

}
