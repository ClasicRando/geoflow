package me.geoflow.api.paths

import io.ktor.http.HttpMethod
import io.ktor.routing.Route
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
import me.geoflow.core.database.tables.Provs

/** Provs API route */
@Suppress("unused")
object ApiProvs : ApiPath(path = "/provs") {

    override fun Route.registerEndpoints() {
        getProvs(this)
    }

    /** Returns a list of all provs available */
    private fun getProvs(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get) { _, connection ->
            val payload = Provs.getRecords(connection)
            ApiResponse.ProvsResponse(payload)
        }
    }

}
