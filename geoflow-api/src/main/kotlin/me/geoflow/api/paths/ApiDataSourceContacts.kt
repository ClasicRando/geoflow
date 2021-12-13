package me.geoflow.api.paths

import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCall
import me.geoflow.core.database.Database
import me.geoflow.core.database.tables.DataSourceContacts
import me.geoflow.core.database.tables.records.DataSourceContact

/** Data source contacts API route */
@Suppress("unused")
object ApiDataSourceContacts : ApiPath(path = "/data-source-contacts") {

    override fun Route.registerEndpoints() {
        getRecords(this)
        updateRecord(this)
        createRecord(this)
    }

    /** Returns a list of data source contacts records */
    private fun getRecords(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Get, path = "/{dsId}") {
            val payload = Database.runWithConnection {
                DataSourceContacts.getRecords(it, call.parameters.getOrFail<Long>("dsId"))
            }
            ApiResponse.DataSourceContactsResponse(payload)
        }
    }

    /** Returns a list of data source contacts records */
    private fun updateRecord(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Put) { userId ->
            val body = call.receive<DataSourceContact>()
            val payload = Database.runWithConnection {
                DataSourceContacts.updateRecord(it, userId, body)
            }
            ApiResponse.DataSourceContactResponse(payload)
        }
    }

    /** Returns a list of data source contacts records */
    private fun createRecord(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Post) { userId ->
            val body = call.receive<DataSourceContact>()
            val payload = Database.runWithConnection {
                DataSourceContacts.createRecord(it, userId, body)
            }
            ApiResponse.InsertIdResponse(payload)
        }
    }

}
