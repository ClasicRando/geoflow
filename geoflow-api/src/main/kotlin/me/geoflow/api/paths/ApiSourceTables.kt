package me.geoflow.api.paths

import me.geoflow.core.database.tables.SourceTables
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
import me.geoflow.core.database.tables.records.SourceTable

/** Source tables API route */
@Suppress("unused")
object ApiSourceTables : ApiPath(path = "/source-tables") {

    override fun Route.registerEndpoints() {
        getSourceTables(this)
        updateSourceTable(this)
        createSourceTable(this)
        deleteSourceTable(this)
    }

    /** Returns list of source table records for the given runId */
    private fun getSourceTables(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/{runId}") { _, connection ->
            val runId = call.parameters.getOrFail<Long>("runId")
            val payload = SourceTables.getRunSourceTables(connection, runId)
            ApiResponse.SourceTablesResponse(payload)
        }
    }

    /**
     * Requests that the specified source table is updated with the entire contents of the provided JSON body. Checks
     * to ensure the requesting user has privileges to update the record in question
     */
    private fun updateSourceTable(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Put) { userOid, connection ->
            val sourceTable = call.receive<SourceTable>()
            val payload = SourceTables.updateSourceTable(connection, userOid, sourceTable)
            ApiResponse.SourceTableResponse(payload)
        }
    }

    /**
     * Requests to create a new source table entry with the entire contents of the provided JSON body for the specified
     * runId. Checks to ensure the requesting user has privileges to create a record for the run
     */
    private fun createSourceTable(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Post, path = "/{runId}") { userOid, connection ->
            val sourceTable = call.receive<SourceTable>()
            val runId = call.parameters.getOrFail<Long>("runId")
            val payload = SourceTables.insertSourceTable(connection, runId, userOid, sourceTable)
            ApiResponse.InsertIdResponse(payload)
        }
    }

    /**
     * Requests that the specified source table is deleted. Checks to ensure the requesting user has privileges to
     * delete the record in question
     */
    private fun deleteSourceTable(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Delete, path = "/{stOid}") { userOid, connection ->
            val stOid = call.parameters.getOrFail<Long>("stOid")
            SourceTables.deleteSourceTable(connection, stOid, userOid)
            ApiResponse.MessageResponse("Deleted source table record ($stOid)")
        }
    }

}
