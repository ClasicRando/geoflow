package me.geoflow.api.paths

import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
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
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/{dsId}") { _, connection ->
            val payload = DataSourceContacts.getRecords(connection, call.parameters.getOrFail<Long>("dsId"))
            ApiResponse.DataSourceContactsResponse(payload)
        }
    }

    /** Returns a list of data source contacts records */
    private fun updateRecord(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Put) { userId, connection ->
            val body = call.receive<DataSourceContact>()
            val payload = DataSourceContacts.updateRecord(connection, userId, body)
            ApiResponse.DataSourceContactResponse(payload)
        }
    }

    /** Returns a list of data source contacts records */
    private fun createRecord(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Post) { userId, connection ->
            val body = call.receive<DataSourceContact>()
            val payload = DataSourceContacts.createRecord(connection, userId, body)
            ApiResponse.InsertIdResponse(payload)
        }
    }

}
