package me.geoflow.api.paths

import me.geoflow.api.utils.apiCall
import me.geoflow.core.database.tables.Actions
import io.ktor.http.HttpMethod
import io.ktor.routing.Route
import me.geoflow.api.utils.ApiResponse

/** User actions API route */
object ApiActions : ApiPath(path = "/actions") {

    override fun Route.registerEndpoints() {
        getActions(this)
    }

    /** Returns list of actions based upon the current user's roles. */
    private fun getActions(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Get) { userOid ->
            val payload = me.geoflow.core.database.Database.runWithConnection {
                Actions.userActions(it, userOid)
            }
            ApiResponse.ActionsResponse(payload)
        }
    }

}