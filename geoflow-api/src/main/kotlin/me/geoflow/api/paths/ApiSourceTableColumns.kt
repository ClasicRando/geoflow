package me.geoflow.api.paths

import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
import me.geoflow.core.database.tables.SourceTableColumns
import me.geoflow.core.database.tables.records.SourceTableColumnUpdate

/** Source table columns API route */
@Suppress("unused")
object ApiSourceTableColumns : ApiPath(path = "/source-table-columns") {

    override fun Route.registerEndpoints() {
        getSourceTableColumns(this)
        getColumnComparisons(this)
        updateColumns(this)
    }

    /** Returns a list of all source table columns for a provided stOid */
    private fun getSourceTableColumns(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/{stOid}") { _, connection ->
            val payload = SourceTableColumns.getRecords(connection, call.parameters.getOrFail<Long>("stOid"))
            ApiResponse.SourceTableColumnsResponse(payload)
        }
    }

    /** Returns a list of column comparisons for a provided stOid */
    private fun getColumnComparisons(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/comparisons/{stOid}") { _, connection ->
            val payload = SourceTableColumns.getComparison(connection, call.parameters.getOrFail<Long>("stOid"))
            ApiResponse.ColumnComparisonsResponse(payload)
        }
    }

    /** Updates a single source table column for a provided stcOid. Returns the new state of the record */
    private fun updateColumns(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Post) { userId, connection ->
            val columnUpdate = call.receive<SourceTableColumnUpdate>()
            val payload = SourceTableColumns.updateRecord(connection, userId, columnUpdate)
            ApiResponse.SourceTableColumnResponse(payload)
        }
    }

}
