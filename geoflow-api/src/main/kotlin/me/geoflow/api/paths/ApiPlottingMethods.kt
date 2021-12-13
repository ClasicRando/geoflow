package me.geoflow.api.paths

import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCall
import me.geoflow.core.database.Database
import me.geoflow.core.database.tables.PlottingMethods
import me.geoflow.core.database.tables.records.PlottingMethod

/** Plotting methods API route */
@Suppress("unused")
object ApiPlottingMethods : ApiPath(path = "/plotting-methods") {

    override fun Route.registerEndpoints() {
        getPlottingMethods(this)
        setPlottingMethods(this)
    }

    /** Returns a list of plotting method records for the given runId */
    private fun getPlottingMethods(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Get, path = "/{runId}") {
            val payload = Database.runWithConnection {
                PlottingMethods.getRecords(it, call.parameters.getOrFail<Long>("runId"))
            }
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
        parent.apiCall(httpMethod = HttpMethod.Post, path = "/{runId}") { userOid ->
            val body = call.receive<List<PlottingMethod>>()
            val (delete, insert) = Database.useTransaction {
                PlottingMethods.setRecords(it, userOid, call.parameters.getOrFail<Long>("runId"), body)
            }
            ApiResponse.MessageResponse("Successful set! Deleted $delete record(s), inserted $insert record(s)")
        }
    }

}
