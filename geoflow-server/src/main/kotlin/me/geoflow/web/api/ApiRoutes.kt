@file:Suppress("TooManyFunctions", "LongMethod")

package me.geoflow.web.api

import io.ktor.http.HttpMethod
import io.ktor.routing.Route
import io.ktor.routing.route
import me.geoflow.core.database.tables.records.DataSourceContact
import me.geoflow.core.database.tables.records.DataSourceRequest
import me.geoflow.core.database.tables.records.GeneratedTableColumn
import me.geoflow.core.database.tables.records.Pipeline
import me.geoflow.core.database.tables.records.PlottingFieldsRequest
import me.geoflow.core.database.tables.records.PlottingMethod
import me.geoflow.core.database.tables.records.RequestUser
import me.geoflow.core.database.tables.records.SourceTable
import me.geoflow.core.database.tables.records.SourceTableColumnUpdate
import me.geoflow.core.mongo.MongoDb

/** Base API route */
fun Route.data() {
    route("/data") {
        operations()
        actions()
        pipelineRuns()
        pipelineRunTasks()
        sourceTables()
        sourceTableColumns()
        users()
        jobs()
        provs()
        roles()
        dataSources()
        dataSourceContacts()
        recordWarehouseTypes()
        pipelines()
        plottingFields()
        plottingMethods()
        plottingMethodTypes()
        generatedTableColumns()
    }
}

/** User operations API route */
private fun Route.operations() {
    route(path = "/operations") {
        /** Returns list of operations based upon the current user's roles. */
        apiCall<NoBody>(apiEndPoint = "/operations", httpMethod = HttpMethod.Get)
        apiCall<NoBody>(path = "/data", apiEndPoint = "/operations/data", httpMethod = HttpMethod.Get)
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

/** Record Warehouse Types API route */
private fun Route.recordWarehouseTypes() {
    route(path = "/rec-warehouse-types") {
        /** Returns list of available provs */
        apiCall<NoBody>(apiEndPoint = "/rec-warehouse-types", httpMethod = HttpMethod.Get)
    }
}

/** Pipelines API route */
private fun Route.pipelines() {
    route(path = "/pipelines") {
        apiCall<NoBody>(
            apiEndPoint = "/pipelines",
            httpMethod = HttpMethod.Get,
        )
        apiCall<NoBody>(
            path = "/{pipelineId}",
            apiEndPoint = "/pipelines/{pipelineId}",
            httpMethod = HttpMethod.Get,
        )
        apiCall<Pipeline>(
            apiEndPoint = "/pipelines",
            httpMethod = HttpMethod.Post,
        )
        apiCall<Pipeline>(
            apiEndPoint = "/pipelines",
            httpMethod = HttpMethod.Put,
        )
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
        apiCall<NoBody>(
            path = "/move-forward/{runId}",
            apiEndPoint = "/pipeline-runs/move-forward/{runId}",
            httpMethod = HttpMethod.Post,
        )
        apiCall<NoBody>(
            path = "/move-back/{runId}",
            apiEndPoint = "/pipeline-runs/move-back/{runId}",
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

/** Data sources API route */
private fun Route.dataSources() {
    route(path = "/data-sources") {
        apiCall<NoBody>(
            path = "/{dsId}",
            apiEndPoint = "data-sources/{dsId}",
            httpMethod = HttpMethod.Get,
        )
        apiCall<NoBody>(
            apiEndPoint = "data-sources",
            httpMethod = HttpMethod.Get,
        )
        apiCall<DataSourceRequest>(
            apiEndPoint = "data-sources",
            httpMethod = HttpMethod.Post,
        )
        apiCall<DataSourceRequest>(
            apiEndPoint = "data-sources",
            httpMethod = HttpMethod.Patch,
        )
    }
}

/** Data source contacts API route */
private fun Route.dataSourceContacts() {
    route(path = "/data-source-contacts") {
        apiCall<NoBody>(
            path = "/{dsId}",
            apiEndPoint = "data-source-contacts/{dsId}",
            httpMethod = HttpMethod.Get,
        )
        apiCall<DataSourceContact>(
            apiEndPoint = "/data-source-contacts",
            httpMethod = HttpMethod.Post,
        )
        apiCall<DataSourceContact>(
            apiEndPoint = "/data-source-contacts",
            httpMethod = HttpMethod.Put,
        )
        apiCall<NoBody>(
            path = "/{contactId}",
            apiEndPoint = "/data-source-contacts/{contactId}",
            httpMethod = HttpMethod.Delete,
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
        apiCall<NoBody>(
            path = "/comparisons/{runId}",
            apiEndPoint = "/source-tables/comparisons/{runId}",
            httpMethod = HttpMethod.Get,
        )
    }
}

/** Source table columns API route */
private fun Route.sourceTableColumns() {
    route(path = "/source-table-columns") {
        apiCall<NoBody>(
            path = "/{stOid}",
            apiEndPoint = "source-table-columns/{stOid}",
            httpMethod = HttpMethod.Get,
        )
        apiCall<NoBody>(
            path = "/comparisons/{stOid}",
            apiEndPoint = "source-table-columns/comparisons/{stOid}",
            httpMethod = HttpMethod.Get,
        )
        apiCall<SourceTableColumnUpdate>(
            apiEndPoint = "source-table-columns",
            httpMethod = HttpMethod.Put,
        )
    }
}

/** Generated table columns API route */
private fun Route.generatedTableColumns() {
    route(path = "/generated-table-columns") {
        apiCall<NoBody>(
            path = "/{stOid}",
            apiEndPoint = "generated-table-columns/{stOid}",
            httpMethod = HttpMethod.Get,
        )
        apiCall<GeneratedTableColumn>(
            apiEndPoint = "generated-table-columns",
            httpMethod = HttpMethod.Post,
        )
        apiCall<GeneratedTableColumn>(
            apiEndPoint = "generated-table-columns",
            httpMethod = HttpMethod.Patch,
        )
        apiCall<NoBody>(
            path = "/{gtcOid}",
            apiEndPoint = "generated-table-columns/{gtcOid}",
            httpMethod = HttpMethod.Delete,
        )
    }
}

/** Plotting fields API route */
private fun Route.plottingFields() {
    route(path = "/plotting-fields") {
        apiCall<NoBody>(
            path = "/{runId}",
            apiEndPoint = "plotting-fields/{runId}",
            httpMethod = HttpMethod.Get,
        )
        apiCall<NoBody>(
            path = "/source-table/{stOid}",
            apiEndPoint = "plotting-fields/source-table/{stOid}",
            httpMethod = HttpMethod.Get,
        )
        apiCall<PlottingFieldsRequest>(
            apiEndPoint = "plotting-fields",
            httpMethod = HttpMethod.Post,
        )
        apiCall<NoBody>(
            path = "/{stOid}",
            apiEndPoint = "plotting-fields/{stOid}",
            httpMethod = HttpMethod.Delete,
        )
    }
}

/** Plotting methods API route */
private fun Route.plottingMethods() {
    route(path = "/plotting-methods") {
        apiCall<NoBody>(
            path = "/{runId}",
            apiEndPoint = "plotting-methods/{runId}",
            httpMethod = HttpMethod.Get,
        )
        apiCall<List<PlottingMethod>>(
            path = "/{runId}",
            apiEndPoint = "plotting-methods/{runId}",
            httpMethod = HttpMethod.Post,
        )
    }
}

/** Plotting method types API route */
private fun Route.plottingMethodTypes() {
    route(path = "/plotting-method-types") {
        apiCall<NoBody>(
            apiEndPoint = "plotting-method-types",
            httpMethod = HttpMethod.Get,
        )
    }
}

/** Internal Users API route */
private fun Route.users() {
    route(path = "/users") {
        apiCall<NoBody>(
            path = "/self",
            apiEndPoint = "/users/self",
            httpMethod = HttpMethod.Get,
        )
        apiCall<NoBody>(
            apiEndPoint = "/users",
            httpMethod = HttpMethod.Get,
        )
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
