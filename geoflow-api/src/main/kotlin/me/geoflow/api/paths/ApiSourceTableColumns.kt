package me.geoflow.api.paths

import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
import me.geoflow.core.database.tables.SourceTableColumns

/** Source table columns API route */
@Suppress("unused")
object ApiSourceTableColumns : ApiPath(path = "/source-table-columns") {

    override fun Route.registerEndpoints() {
        getSourceTableColumns(this)
    }

    /** Returns a list of all source table columns for a provided stOid */
    private fun getSourceTableColumns(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/{stOid}") { _, connection ->
            val payload = SourceTableColumns.getRecords(connection, call.parameters.getOrFail<Long>("stOid"))
            ApiResponse.SourceTableColumnsResponse(payload)
        }
    }

}
