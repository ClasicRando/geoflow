package me.geoflow.api.paths

import me.geoflow.core.database.tables.InternalUsers
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.getOrFail
import me.geoflow.api.utils.ApiResponse
import me.geoflow.api.utils.apiCallPostgres
import me.geoflow.core.database.tables.records.RequestUser

/** Internal Users API route */
@Suppress("unused")
object ApiUsers : ApiPath(path = "/users") {

    override fun Route.registerEndpoints() {
        getUser(this)
        getUsers(this)
        createUser(this)
        updateUser(this)
        hasRole(this)
        getCollectionUsers(this)
    }

    /** Returns a single user record if the caller is the user */
    private fun hasRole(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/has-role/{role}") { userOid, connection ->
            val role = call.parameters.getOrFail("role")
            InternalUsers.requireRole(connection, userOid, role)
            ApiResponse.MessageResponse("User has role, '$role'")
        }
    }

    /** Returns a single user record if the caller is the user */
    private fun getUser(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/self") { userOid, connection ->
            val payload = InternalUsers.getSelf(connection, userOid)
            ApiResponse.UserResponse(payload)
        }
    }

    /** Returns list of user records */
    private fun getUsers(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get) { userOid, connection ->
            val payload = InternalUsers.getUsers(connection, userOid)
            ApiResponse.UsersResponse(payload)
        }
    }

    /** Returns list of user records that have the collection role */
    private fun getCollectionUsers(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Get, path = "/collection") { userOid, connection ->
            val payload = InternalUsers.getCollectionUsers(connection, userOid)
            ApiResponse.CollectionUsersResponse(payload)
        }
    }

    /**
     * Requests to create a new user entry with the entire contents of the provided JSON body. Checks to ensure the
     * requesting user has privileges to create a new user
     */
    private fun createUser(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Post) { userOid, connection ->
            val requestUser = call.receive<RequestUser>()
            val newUserOid = InternalUsers.createUser(connection, requestUser, userOid)
            ApiResponse.InsertIdResponse(newUserOid)
        }
    }

    /**
     * Requests that the specified user is updated in part with the contents of the provided JSON body. Checks to ensure
     * the requesting user has privileges to update users
     */
    private fun updateUser(parent: Route) {
        parent.apiCallPostgres(httpMethod = HttpMethod.Put) { userOid, connection ->
            val requestUser = call.receive<RequestUser>()
            val payload = InternalUsers.updateUser(connection, requestUser, userOid)
            ApiResponse.UserResponse(payload)
        }
    }

}
