package me.geoflow.api.paths

import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
import me.geoflow.core.database.tables.PlottingMethods
import me.geoflow.core.database.tables.records.PlottingMethodRequest

/** Plotting methods API route */
@Suppress("unused")
object ApiPlottingMethods : ApiPath(path = "/plotting-methods") {

    override fun Route.registerEndpoints() {
        getPlottingMethods(this)
        setPlottingMethods(this)
    }

    /** Returns a list of plotting method records for the given runId */
    private fun getPlottingMethods(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/{runId}") { _, connection ->
            val payload = PlottingMethods.getRecords(connection, call.parameters.getOrFail<Long>("runId"))
            ApiResponse.PlottingMethodsResponse(payload)
        }
    }

    /**
     * Attempts to set the plotting methods for a given runId using the body of the request.
     *
     * Uses a transaction to delete all methods for the runId then attempts to insert the list of objects in the request
     * body. If the insert fails, the changes are rolled back to preserve the current state of the runId.
     */
    private fun setPlottingMethods(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Post, path = "/{runId}") { userOid, connection ->
            val body = call.receive<List<PlottingMethodRequest>>()
            val (delete, insert) = PlottingMethods.setRecords(
                connection,
                userOid,
                call.parameters.getOrFail<Long>("runId"),
                body,
            )
            val payload = "Successful set! Deleted $delete record(s), inserted $insert record(s)"
            ApiResponse.MessageResponse(payload)
        }
    }

}
