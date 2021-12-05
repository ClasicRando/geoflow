package paths

import utils.apiCall
import database.Database
import database.tables.Actions
import io.ktor.http.HttpMethod
import io.ktor.routing.Route
import utils.ApiResponse

/** User actions API route */
object ApiActions : ApiPath(path = "/actions") {

    override fun Route.registerEndpoints() {
        getActions(this)
    }

    /** Returns list of actions based upon the current user's roles. */
    private fun getActions(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Get) { userOid ->
            val payload = Database.runWithConnection {
                Actions.userActions(it, userOid)
            }
            ApiResponse.ActionsResponse(payload)
        }
    }

}
