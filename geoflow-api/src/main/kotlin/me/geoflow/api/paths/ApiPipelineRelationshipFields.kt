package me.geoflow.api.paths

import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
import me.geoflow.core.database.tables.PipelineRelationshipFields

/** */
@Suppress("unused")
object ApiPipelineRelationshipFields : ApiPath("/pipeline-relationship-fields") {

    override fun Route.registerEndpoints() {
        getFields(this)
    }

    /** */
    private fun getFields(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/{stOid}") { _, connection ->
            val stOid = call.parameters.getOrFail<Long>("stOid")
            val payload = PipelineRelationshipFields.getRecords(connection, stOid)
            ApiResponse.PipelineRelationshipFieldsResponse(payload)
        }
    }

}
