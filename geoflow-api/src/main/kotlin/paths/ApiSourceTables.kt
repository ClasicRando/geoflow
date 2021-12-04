package paths

import apiCall
import database.Database
import database.tables.SourceTables
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.getOrFail

/** Source tables API route */
object ApiSourceTables : ApiPath(path = "/source-tables") {

    override fun Route.registerEndpoints() {
        getSourceTables(this)
        updateSourceTable(this)
        createSourceTable(this)
        deleteSourceTable(this)
    }

    private fun getSourceTables(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Get, path = "/{runId}") {
            val runId = call.parameters.getOrFail("runId").toLong()
            val payload = Database.runWithConnection {
                SourceTables.getRunSourceTables(it, runId)
            }
            ApiResponse.SourceTablesResponse(payload)
        }
    }

    private fun updateSourceTable(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Put) { userOid ->
            val sourceTable = call.receive<SourceTables.Record>()
            val payload = Database.useTransaction {
                SourceTables.updateSourceTable(it, userOid, sourceTable)
            }
            ApiResponse.SourceTableResponse(payload)
        }
    }

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
