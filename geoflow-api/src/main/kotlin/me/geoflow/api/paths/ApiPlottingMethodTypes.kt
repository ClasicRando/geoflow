package me.geoflow.api.paths

import io.ktor.http.HttpMethod
import io.ktor.routing.Route
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCall
import me.geoflow.core.database.Database
import me.geoflow.core.database.tables.PlottingMethodTypes

/** Plotting method types API route */
@Suppress("unused")
object ApiPlottingMethodTypes : ApiPath(path = "/plotting-method-types") {

    override fun Route.registerEndpoints() {
        getTypes(this)
    }

    /** Returns a list of all plotting method types available */
    private fun getTypes(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Get) {
            val payload = Database.runWithConnection {
                PlottingMethodTypes.getRecords(it)
            }
            ApiResponse.PlottingMethodTypesResponse(payload)
        }
    }
}
