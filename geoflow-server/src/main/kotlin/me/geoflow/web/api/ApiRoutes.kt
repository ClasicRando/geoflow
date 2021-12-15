@file:Suppress("TooManyFunctions", "LongMethod")

package me.geoflow.web.api

import io.ktor.http.HttpMethod
import io.ktor.routing.Route
import io.ktor.routing.route
import me.geoflow.core.database.tables.records.RequestUser
import me.geoflow.core.database.tables.records.SourceTable
import me.geoflow.core.mongo.MongoDb

/** Base API route */
fun Route.data() {
    route("/data") {
        operations()
        actions()
        pipelineRuns()
        pipelineRunTasks()
        sourceTables()
        users()
        jobs()
        provs()
        roles()
    }
}

/** User operations API route */
private fun Route.operations() {
    route(path = "/operations") {
        /** Returns list of operations based upon the current user's roles. */
        apiCall<NoBody>(apiEndPoint = "/operations", httpMethod = HttpMethod.Get)
    }
}

/** User actions API route */
private fun Route.actions() {
    route(path = "/actions") {
        /** Returns list of actions based upon the current user's roles. */
        apiCall<NoBody>(apiEndPoint = "/actions", httpMethod = HttpMethod.Get)
    }
}

/** Roles API route */
private fun Route.roles() {
    route(path = "/roles") {
        /** Returns list of available roles */
        apiCall<NoBody>(apiEndPoint = "/roles", httpMethod = HttpMethod.Get)
    }
}

/** Provs API route */
private fun Route.provs() {
    route(path = "/provs") {
        /** Returns list of available provs */
        apiCall<NoBody>(apiEndPoint = "/provs", httpMethod = HttpMethod.Get)
    }
}

/** Pipeline runs API route */
private fun Route.pipelineRuns() {
    route(path = "/pipeline-runs") {
        /** Returns list of pipeline runs for the given workflow code based upon the current user. */
        apiCall<NoBody>(
            path = "/{code}",
            apiEndPoint = "/pipeline-runs/{code}",
            httpMethod = HttpMethod.Get,
        )
        apiCall<NoBody>(
            path = "/pickup/{runId}",
            apiEndPoint = "/pipeline-runs/pickup/{runId}",
            httpMethod = HttpMethod.Post,
        )
    }
}

/** Pipeline run tasks API route */
private fun Route.pipelineRunTasks() {
    route(path = "/pipeline-run-tasks") {
        publisher(path = "/{runId}", endPoint = "pipeline-run-tasks/{runId}")
        apiCall<NoBody>(
            path = "/reset-task/{prTaskId}",
            apiEndPoint = "/pipeline-run-tasks/reset-task/{prTaskId}",
            httpMethod = HttpMethod.Post,
        )
        apiCall<NoBody>(
            path = "/run-next/{runId}",
            apiEndPoint = "/pipeline-run-tasks/run-next/{runId}",
            httpMethod = HttpMethod.Post,
        )
        apiCall<NoBody>(
            path = "/run-all/{runId}",
            apiEndPoint = "/pipeline-run-tasks/run-all/{runId}",
            httpMethod = HttpMethod.Post,
        )
    }
}

/** Source tables API route */
private fun Route.sourceTables() {
    route(path = "/source-tables") {
        apiCall<NoBody>(
            path = "/{runId}",
            apiEndPoint = "source-tables/{runId}",
            httpMethod = HttpMethod.Get,
        )
        apiCall<SourceTable>(
            apiEndPoint = "/source-tables",
            httpMethod = HttpMethod.Put,
        )
        apiCall<SourceTable>(
            path = "/{runId}",
            apiEndPoint = "/source-tables/{runId}",
            httpMethod = HttpMethod.Post,
        )
        apiCall<NoBody>(
            path = "/{stOid}",
            apiEndPoint = "/source-tables/{stOid}",
            httpMethod = HttpMethod.Delete,
        )
    }
}

/** Internal Users API route */
private fun Route.users() {
    route(path = "/users") {
        apiCall<NoBody>(apiEndPoint = "/users", httpMethod = HttpMethod.Get)
        apiCall<RequestUser>(
            apiEndPoint = "/users",
            httpMethod = HttpMethod.Post,
        )
        apiCall<RequestUser>(
            apiEndPoint = "/users",
            httpMethod = HttpMethod.Patch,
        )
    }
}

/** All KJob data API route */
private fun Route.jobs() {
    route(path = "/jobs") {
        kjobTasks()
    }
}

/** KJob pipeline run tasks API route */
private fun Route.kjobTasks() {
    route(path = "/tasks") {
        apiCall<MongoDb.TaskApiRequest>(apiEndPoint = "/jobs/tasks", httpMethod = HttpMethod.Get)
    }
}
