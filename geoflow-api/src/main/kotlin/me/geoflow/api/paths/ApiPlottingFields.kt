package me.geoflow.api.paths

import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
import me.geoflow.core.database.tables.PlottingFields
import me.geoflow.core.database.tables.records.PlottingFieldsRequest

/** Plotting fields API route */
@Suppress("unused")
object ApiPlottingFields : ApiPath("/plotting-fields") {

    override fun Route.registerEndpoints() {
        getPlottingFields(this)
        setPlottingFields(this)
        deletePlottingFields(this)
    }

    /** Returns a list of plotting field record for the given runId */
    private fun getPlottingFields(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/{runId}") { _, connection ->
            val payload = PlottingFields.getRecords(connection, call.parameters.getOrFail<Long>("runId"))
            ApiResponse.PlottingFieldsResponse(payload)
        }
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/source-table/{stOid}") { _, connection ->
            val stOid = call.parameters.getOrFail<Long>("stOid")
            val payload = PlottingFields.getSourceTableRecords(connection, stOid)
            ApiResponse.PlottingFieldsResponse(payload)
        }
    }

    /** Attempts to create a new plotting field record with the provided body */
    private fun setPlottingFields(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Post) { userOid, connection ->
            val body = call.receive<PlottingFieldsRequest>()
            PlottingFields.setRecord(connection, userOid, body)
            val payload = "Successfully set record for st_oid = ${body.stOid}"
            ApiResponse.MessageResponse(payload)
        }
    }

    /** Attempts to delete an existing plotting field record with the provided runId and fileId in the path */
    private fun deletePlottingFields(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Delete, path = "/{stOid}") { userOid, connection ->
            val stOid = call.parameters.getOrFail<Long>("stOid")
            PlottingFields.deleteRecord(connection, userOid, stOid)
            val payload = "Successfully deleted record for st_oid = $stOid"
            ApiResponse.MessageResponse(payload)
        }
    }

}
