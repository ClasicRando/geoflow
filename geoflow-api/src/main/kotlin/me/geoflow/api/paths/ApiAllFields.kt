package me.geoflow.api.paths

import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.tables.GeneratedTableColumns
import me.geoflow.core.database.tables.SourceTableColumns
import me.geoflow.core.database.tables.records.LogicField

/** */
@Suppress("unused")
object ApiAllFields : ApiPath("/all-fields") {

    override fun Route.registerEndpoints() {
        getAllFields(this)
    }

    private fun getAllFields(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/{stOid}") { _, connection ->
            val stOid = call.parameters.getOrFail<Long>("stOid")
            val payload = connection.submitQuery<LogicField>(
                sql = """
                    SELECT stc_oid, FALSE, name
                    FROM   ${SourceTableColumns.tableName}
                    WHERE  st_oid = ?
                    UNION ALL
                    SELECT gtc_oid, TRUE, name
                    FROM   ${GeneratedTableColumns.tableName}
                    WHERE  st_oid = ?
                """.trimIndent(),
                stOid,
                stOid,
            )
            ApiResponse.LogicFieldsResponse(payload)
        }
    }

}
