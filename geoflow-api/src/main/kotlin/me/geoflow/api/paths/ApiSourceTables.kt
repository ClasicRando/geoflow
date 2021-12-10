package me.geoflow.api.paths

import me.geoflow.api.utils.apiCall
import me.geoflow.core.database.tables.SourceTables
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.core.database.Database

/** Source tables API route */
object ApiSourceTables : ApiPath(path = "/source-tables") {

    override fun Route.registerEndpoints() {
        getSourceTables(this)
        updateSourceTable(this)
        createSourceTable(this)
        deleteSourceTable(this)
    }

    /** Returns list of source table records for the given runId */
    private fun getSourceTables(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Get, path = "/{runId}") {
            val runId = call.parameters.getOrFail("runId").toLong()
            val payload = Database.runWithConnection {
                SourceTables.getRunSourceTables(it, runId)
            }
            ApiResponse.SourceTablesResponse(payload)
        }
    }

    /**
     * Requests that the specified source table is updated with the entire contents of the provided JSON body. Checks
     * to ensure the requesting user has privileges to update the record in question
     */
    private fun updateSourceTable(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Put) { userOid ->
            val sourceTable = call.receive<SourceTables.Record>()
            val payload = Database.useTransaction {
                SourceTables.updateSourceTable(it, userOid, sourceTable)
            }
            ApiResponse.SourceTableResponse(payload)
        }
    }

    /**
     * Requests to create a new source table entry with the entire contents of the provided JSON body for the specified
     * runId. Checks to ensure the requesting user has privileges to create a record for the run
     */
    private fun createSourceTable(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Post, path = "/{runId}") { userOid ->
            val sourceTable = call.receive<SourceTables.Record>()
            val runId = call.parameters.getOrFail("runId").toLong()
            val payload = Database.runWithConnection {
                SourceTables.insertSourceTable(it, runId, userOid, sourceTable)
            }
            ApiResponse.InsertIdResponse(payload)
        }
    }

    /**
     * Requests that the specified source table is deleted. Checks to ensure the requesting user has privileges to
     * delete the record in question
     */
    private fun deleteSourceTable(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Delete, path = "/{stOid}") { userOid ->
            val stOid = call.parameters.getOrFail("stOid").toLong()
            Database.runWithConnection {
                SourceTables.deleteSourceTable(it, stOid, userOid)
            }
            ApiResponse.MessageResponse("Deleted source table record ($stOid)")
        }
    }

}
