package me.geoflow.api.paths

import io.ktor.http.HttpMethod
import io.ktor.routing.Route
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
import me.geoflow.core.database.tables.Roles

/** Roles API route */
@Suppress("unused")
object ApiRoles : ApiPath(path = "/roles") {

    override fun Route.registerEndpoints() {
        getRoles(this)
    }

    /** Returns a list of all roles available */
    private fun getRoles(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get) { _, connection ->
            val payload = Roles.getRecords(connection)
            ApiResponse.RolesResponse(payload)
        }
    }

}
