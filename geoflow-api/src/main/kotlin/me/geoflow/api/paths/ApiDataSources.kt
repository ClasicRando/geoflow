package me.geoflow.api.paths

import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCall
import me.geoflow.core.database.Database
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
        parent.apiCall(httpMethod = HttpMethod.Get, path = "/{dsId}") {
            val payload = Database.runWithConnection {
                DataSources.getRecord(it, call.parameters.getOrFail<Long>("dsId"))
            }
            ApiResponse.DataSourceResponse(payload)
        }
    }

    /** Returns a list of data sources records */
    private fun getRecords(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Get) { userId ->
            val payload = Database.runWithConnection {
                DataSources.getRecords(it, userId)
            }
            ApiResponse.DataSourcesResponse(payload)
        }
    }

    /** Attempts to update a data source record partially with the body of the request */
    private fun updateRecord(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Patch) { userId ->
            val body = call.receive<DataSourceRequest>()
            val payload = Database.runWithConnection {
                DataSources.updateRecord(it, userId, body)
            }
            ApiResponse.DataSourceResponse(payload)
        }
    }

    /** Attempts to create a new data source record with the body of the request */
    private fun createRecord(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Post) { userId ->
            val body = call.receive<DataSourceRequest>()
            val payload = Database.runWithConnection {
                DataSources.createRecord(it, userId, body)
            }
            ApiResponse.InsertIdResponse(payload)
        }
    }

}
