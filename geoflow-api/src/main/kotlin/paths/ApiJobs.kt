package paths

import apiCall
import database.Database
import database.tables.InternalUsers
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.routing.Route
import kotlinx.coroutines.flow.toList
import mongo.MongoDb

/** All KJob data API route */
object ApiJobs : ApiPath(path = "/jobs") {

    override fun Route.registerEndpoints() {
        getKjobTasks(this)
    }

    /** KJob pipeline run tasks API route */
    private fun getKjobTasks(parent: Route) {
        parent.apiCall(httpMethod = HttpMethod.Get, path = "/tasks") { userOid ->
            Database.runWithConnection { InternalUsers.requireAdmin(it, userOid) }
            val request = call.receive<MongoDb.TaskApiRequest>()
            val payload = MongoDb.getTasks(request).toList()
            ApiResponse.KJobTasksResponse(payload)
        }
    }

}
