package me.geoflow.api.paths

import me.geoflow.api.utils.apiCall
import me.geoflow.core.database.tables.InternalUsers
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import me.geoflow.api.utils.ApiResponse
import me.geoflow.core.database.Database

/** Internal Users API route */
object ApiUsers : ApiPath(path = "/users") {

    override fun Route.registerEndpoints() {
        getUser(this)
        getUsers(this)
        createUser(this)
        updateUser(this)
    }

    /** Returns a single user record if the caller is the user */
    private fun getUser(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Get, path = "/self") { userOid ->
            val payload = Database.runWithConnection {
                InternalUsers.getSelf(it, userOid)
            }
            ApiResponse.UserResponse(payload)
        }
    }

    /** Returns list of user records */
    private fun getUsers(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Get) { userOid ->
            val payload = Database.runWithConnection {
                InternalUsers.getUsers(it, userOid)
            }
            ApiResponse.UsersResponse(payload)
        }
    }

    /**
     * Requests to create a new user entry with the entire contents of the provided JSON body. Checks to ensure the
     * requesting user has privileges to create a new user
     */
    private fun createUser(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Post) { userOid ->
            val requestUser = call.receive<InternalUsers.RequestUser>()
            val newUserOid = Database.runWithConnection {
                InternalUsers.createUser(it, requestUser, userOid)
            }
            ApiResponse.InsertIdResponse(newUserOid)
        }
    }

    /**
     * Requests that the specified user is updated in part with the contents of the provided JSON body. Checks to ensure
     * the requesting user has privileges to update users
     */
    private fun updateUser(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Put) { userOid ->
            val requestUser = call.receive<InternalUsers.RequestUser>()
            val payload = Database.runWithConnection {
                InternalUsers.updateUser(it, requestUser, userOid)
            }
            ApiResponse.UserResponse(payload)
        }
    }

}
