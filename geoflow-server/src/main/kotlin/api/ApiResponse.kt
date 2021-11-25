package api

import database.tables.Actions
import database.tables.InternalUsers
import database.tables.PipelineRuns
import database.tables.SourceTables
import database.tables.WorkflowOperations
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mongo.MongoDb

/** Sealed definition of any API response */
sealed interface ApiResponse {

    /** */
    interface Success<T>: ApiResponse {
        /** type of response object */
        val responseObject: String
        /** payload of response as a single object */
        val payload: T
    }

    /** API response for a list of records from [workflow_operations][WorkflowOperations] */
    @Serializable
    class OperationsResponse(
        override val payload: List<WorkflowOperations.WorkflowOperation>,
    ): Success<List<WorkflowOperations.WorkflowOperation>> {
        @SerialName("object")
        override val responseObject: String = "operation"
    }

    /** API response for a list of records from [actions][Actions] */
    @Serializable
    class ActionsResponse(
        override val payload: List<Actions.Action>,
    ): Success<List<Actions.Action>> {
        @SerialName("object")
        override val responseObject: String = "action"
    }

    /** API response for a list of records from [pipeline_runs][PipelineRuns] */
    @Serializable
    class PipelineRunsResponse(
        override val payload: List<PipelineRuns.Record>,
    ): Success<List<PipelineRuns.Record>> {
        @SerialName("object")
        override val responseObject: String = "pipeline_run"
    }

    /** API response for a list of records from [source_tables][SourceTables] */
    @Serializable
    class SourceTablesResponse(
        override val payload: List<SourceTables.Record>,
    ): Success<List<SourceTables.Record>> {
        @SerialName("object")
        override val responseObject: String = "source_table"
    }

    /** API response for a list of records from [internal_users][InternalUsers] */
    @Serializable
    class UsersResponse(
        override val payload: List<InternalUsers.User>,
    ): Success<List<InternalUsers.User>> {
        @SerialName("object")
        override val responseObject: String = "internal_user"
    }

    /** API response for a list of kjob tasks */
    @Serializable
    class KJobTasksResponse(
        override val payload: List<MongoDb.ScheduledJob>,
    ): Success<List<MongoDb.ScheduledJob>> {
        @SerialName("object")
        override val responseObject: String = "kjob_task"
    }

    /** API response with a single serializable object in the payload */
    @Serializable
    data class SuccessSingle<T>(
        /** type of response object */
        @SerialName("object")
        val responseObject: String,
        /** payload of response as a single object */
        val payload: T,
    ): ApiResponse

    /** API error response, contains details of error */
    @Serializable
    data class Error(
        /** error code */
        val code: Int,
        /** details about throwables from the error */
        val errors: ApiErrors,
    ): ApiResponse
}
