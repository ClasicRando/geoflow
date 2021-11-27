package api

import database.tables.Actions
import database.tables.InternalUsers
import database.tables.PipelineRunTasks
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
        override val payload: List<PipelineRuns.PipelineRun>,
    ): Success<List<PipelineRuns.PipelineRun>> {
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

    /** API response for a list of records from [source_tables][SourceTables] */
    @Serializable
    class SourceTableResponse(
        override val payload: SourceTables.Record,
    ): Success<SourceTables.Record> {
        @SerialName("object")
        override val responseObject: String = "source_table"
    }

    /** API response for a list of records from [internal_users][InternalUsers] */
    @Serializable
    class UsersResponse(
        override val payload: List<InternalUsers.ResponseUser>,
    ): Success<List<InternalUsers.ResponseUser>> {
        @SerialName("object")
        override val responseObject: String = "internal_user"
    }

    /** API response for a list of records from [internal_users][InternalUsers] */
    @Serializable
    class UserResponse(
        override val payload: InternalUsers.RequestUser,
    ): Success<InternalUsers.RequestUser> {
        @SerialName("object")
        override val responseObject: String = "request_user"
    }

    /** API response for a list of kjob tasks */
    @Serializable
    class KJobTasksResponse(
        override val payload: List<MongoDb.ScheduledJob>,
    ): Success<List<MongoDb.ScheduledJob>> {
        @SerialName("object")
        override val responseObject: String = "kjob_task"
    }

    /** API response for a next task scheduled to run */
    @Serializable
    class NextTaskResponse(
        override val payload: PipelineRunTasks.NextTask,
    ): Success<PipelineRunTasks.NextTask> {
        @SerialName("object")
        override val responseObject: String = "next_task"
    }

    /** API response for an inserted record */
    @Serializable
    class InsertIdResponse(
        override val payload: Long,
    ): Success<Long> {
        @SerialName("object")
        override val responseObject: String = "new_record_id"
    }

    /** API response for a successful action */
    @Serializable
    class MessageResponse(
        override val payload: String,
    ): Success<String> {
        @SerialName("object")
        override val responseObject: String = "message"
    }

    /** API error response, contains details of error */
    @Serializable
    data class Error(
        /** error code */
        val code: Int,
        /** details about throwables from the error */
        val errors: ApiErrors,
    ): ApiResponse
}
