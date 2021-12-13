package me.geoflow.api.paths

import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
import me.geoflow.core.database.tables.PlottingFields
import me.geoflow.core.database.tables.records.PlottingFieldBody

/** Plotting fields API route */
@Suppress("unused")
object ApiPlottingFields : ApiPath("/plotting-fields") {

    override fun Route.registerEndpoints() {
        getPlottingFields(this)
        createPlottingFields(this)
        updatePlottingFields(this)
        deletePlottingFields(this)
    }

    /** Returns a list of plotting field record for the given runId */
    private fun getPlottingFields(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/{runId}") { _, connection ->
            val payload = PlottingFields.getRecords(connection, call.parameters.getOrFail<Long>("runId"))
            ApiResponse.PlottingFieldsResponse(payload)
        }
    }

    /** Attempts to create a new plotting field record with the provided body */
    private fun createPlottingFields(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Post) { userOid, connection ->
            val body = call.receive<PlottingFieldBody>()
            PlottingFields.createRecord(connection, userOid, body)
            val payload = "Successfully created record for runId = ${body.runId} & file_id = ${body.fileId}"
            ApiResponse.MessageResponse(payload)
        }
    }

    /** Attempts to perform a full update on an existing plotting field record with the provided body */
    private fun updatePlottingFields(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Put) { userOid, connection ->
            val body = call.receive<PlottingFieldBody>()
            val payload = PlottingFields.updateRecord(connection, userOid, body)
            ApiResponse.PlottingFieldsSingle(payload)
        }
    }

    /** Attempts to delete an existing plotting field record with the provided runId and fileId in the path */
    private fun deletePlottingFields(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Delete, path = "/{runId}/{fileId}") { userOid, connection ->
            val runId = call.parameters.getOrFail<Long>("runId")
            val fileId = call.parameters.getOrFail("fileId")
            PlottingFields.deleteRecord(connection, userOid, runId, fileId)
            val payload = "Successfully deleted record for runId = $runId & file_id = $fileId"
            ApiResponse.MessageResponse(payload)
        }
    }

}
