package me.geoflow.api.paths

import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCall
import me.geoflow.core.database.Database
import me.geoflow.core.database.tables.PlottingFields
import me.geoflow.core.database.tables.records.PlottingFieldBody

/** Plotting fields API route */
object ApiPlottingFields : ApiPath("/plotting-fields") {

    override fun Route.registerEndpoints() {
        getPlottingFields(this)
        createPlottingFields(this)
        updatePlottingFields(this)
        deletePlottingFields(this)
    }

    /** Returns a list of plotting field record for the given runId */
    private fun getPlottingFields(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Get, path = "/{runId}") {
            val payload = Database.runWithConnection {
                PlottingFields.getRecords(it, call.parameters.getOrFail<Long>("runId"))
            }
            ApiResponse.PlottingFieldsResponse(payload)
        }
    }

    /** Attempts to create a new plotting field record with the provided body */
    private fun createPlottingFields(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Post) { userOid ->
            val body = call.receive<PlottingFieldBody>()
            Database.runWithConnection { PlottingFields.createRecord(it, userOid, body) }
            ApiResponse.MessageResponse(
                "Successfully created record for runId = ${body.runId} & file_id = ${body.fileId}"
            )
        }
    }

    /** Attempts to perform a full update on an existing plotting field record with the provided body */
    private fun updatePlottingFields(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Put) { userOid ->
            val body = call.receive<PlottingFieldBody>()
            val payload = Database.runWithConnection {
                PlottingFields.updateRecord(it, userOid, body)
            }
            ApiResponse.PlottingFieldsSingle(payload)
        }
    }

    /** Attempts to delete an existing plotting field record with the provided runId and fileId in the path */
    private fun deletePlottingFields(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Delete, path = "/{runId}/{fileId}") { userOid ->
            val runId = call.parameters.getOrFail<Long>("runId")
            val fileId = call.parameters.getOrFail("fileId")
            Database.runWithConnection {
                PlottingFields.deleteRecord(it, userOid, runId, fileId)
            }
            ApiResponse.MessageResponse("Successfully deleted record for runId = $runId & file_id = $fileId")
        }
    }

}
