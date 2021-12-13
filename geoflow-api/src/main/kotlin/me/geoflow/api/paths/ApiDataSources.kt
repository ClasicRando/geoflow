package me.geoflow.api.paths

import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
import me.geoflow.core.database.tables.DataSources
import me.geoflow.core.database.tables.records.DataSourceRequest

/** Data sources API route */
@Suppress("unused")
object ApiDataSources : ApiPath(path = "/data-sources") {

    override fun Route.registerEndpoints() {
        getRecord(this)
        getRecords(this)
        updateRecord(this)
        createRecord(this)
    }

    /** Returns a data source record for the given dsId */
    private fun getRecord(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/{dsId}") { _, connection ->
            val payload = DataSources.getRecord(connection, call.parameters.getOrFail<Long>("dsId"))
            ApiResponse.DataSourceResponse(payload)
        }
    }

    /** Returns a list of data sources records */
    private fun getRecords(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get) { userId, connection ->
            val payload = DataSources.getRecords(connection, userId)
            ApiResponse.DataSourcesResponse(payload)
        }
    }

    /** Attempts to update a data source record partially with the body of the request */
    private fun updateRecord(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Patch) { userId, connection ->
            val body = call.receive<DataSourceRequest>()
            val payload = DataSources.updateRecord(connection, userId, body)
            ApiResponse.DataSourceResponse(payload)
        }
    }

    /** Attempts to create a new data source record with the body of the request */
    private fun createRecord(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Post) { userId, connection ->
            val body = call.receive<DataSourceRequest>()
            val payload = DataSources.createRecord(connection, userId, body)
            ApiResponse.InsertIdResponse(payload)
        }
    }

}
