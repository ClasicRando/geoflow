package me.geoflow.api.paths

import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
import me.geoflow.core.database.tables.GeneratedTableColumns
import me.geoflow.core.database.tables.records.GeneratedTableColumn

/** */
@Suppress("unused")
object ApiGeneratedTableColumns : ApiPath("/generated-table-columns") {

    override fun Route.registerEndpoints() {
        getGeneratedTableColumns(this)
        createdGeneratedTableColumn(this)
        updateGeneratedTableColumn(this)
        deleteGeneratedTableColumns(this)
    }

    /** Returns a list of all generated columns for a provided stOid */
    private fun getGeneratedTableColumns(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/{stOid}") { _, connection ->
            val payload = GeneratedTableColumns.getRecords(connection, call.parameters.getOrFail<Long>("stOid"))
            ApiResponse.GeneratedTableColumnsResponse(payload)
        }
    }

    /**  */
    private fun createdGeneratedTableColumn(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Post) { userId, connection ->
            val newColumn = call.receive<GeneratedTableColumn>()
            val payload = GeneratedTableColumns.createRecord(connection, userId, newColumn)
            ApiResponse.InsertIdResponse(payload)
        }
    }

    /**  */
    private fun updateGeneratedTableColumn(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Patch) { userId, connection ->
            val columnUpdate = call.receive<GeneratedTableColumn>()
            val payload = GeneratedTableColumns.updateRecord(connection, userId, columnUpdate)
            ApiResponse.GeneratedTableColumnResponse(payload)
        }
    }

    /**  */
    private fun deleteGeneratedTableColumns(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Delete, path = "/{gtcOid}") { userId, connection ->
            val columnId = call.parameters.getOrFail<Long>("gtcOid")
            GeneratedTableColumns.deleteRecord(connection, userId, columnId)
            ApiResponse.MessageResponse("Successfully delete record for ")
        }
    }

}
