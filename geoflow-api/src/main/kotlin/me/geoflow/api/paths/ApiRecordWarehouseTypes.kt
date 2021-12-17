package me.geoflow.api.paths

import io.ktor.http.HttpMethod
import io.ktor.routing.Route
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
import me.geoflow.core.database.tables.RecordWarehouseTypes

/** Record warehouse types API route */
@Suppress("unused")
object ApiRecordWarehouseTypes : ApiPath(path = "/rec-warehouse-types") {

    override fun Route.registerEndpoints() {
        getTypes(this)
    }

    /** Returns a list of all warehouse types available */
    private fun getTypes(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get) { _, connection ->
            val payload = RecordWarehouseTypes.getRecords(connection)
            ApiResponse.RecordWarehouseTypesResponse(payload)
        }
    }

}
