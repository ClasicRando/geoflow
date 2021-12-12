package me.geoflow.api.utils

import me.geoflow.core.database.tables.Actions
import me.geoflow.core.database.tables.InternalUsers
import me.geoflow.core.database.tables.PipelineRuns
import me.geoflow.core.database.tables.PlottingFields
import me.geoflow.core.database.tables.PlottingMethods
import me.geoflow.core.database.tables.SourceTables
import me.geoflow.core.database.tables.WorkflowOperations
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.geoflow.core.database.tables.records.Action
import me.geoflow.core.database.tables.records.NextTask
import me.geoflow.core.database.tables.records.PipelineRun
import me.geoflow.core.database.tables.records.PlottingFieldBody
import me.geoflow.core.database.tables.records.PlottingMethod
import me.geoflow.core.database.tables.records.RequestUser
import me.geoflow.core.database.tables.records.ResponseUser
import me.geoflow.core.database.tables.records.SourceTable
import me.geoflow.core.database.tables.records.WorkflowOperation
import me.geoflow.core.mongo.MongoDb

/** Sealed definition of any API response */
sealed interface ApiResponse {

    /** Template for a successful API response. Contains a payload of type [T] and a name of the payload object type */
    interface Success<T>: ApiResponse {
        /** type of response object */
        val responseObject: String
        /** payload of response as a single object */
        val payload: T
    }

    /** API response for a list of records from [workflow_operations][WorkflowOperations] */
    @Serializable
    class OperationsResponse(
        override val payload: List<WorkflowOperation>,
    ): Success<List<WorkflowOperation>> {
        @SerialName("object")
        override val responseObject: String = "operation"
    }

    /** API response for a list of records from [actions][Actions] */
    @Serializable
    class ActionsResponse(
        override val payload: List<Action>,
    ): Success<List<Action>> {
        @SerialName("object")
        override val responseObject: String = "action"
    }

    /** API response for a list of records from [pipeline_runs][PipelineRuns] */
    @Serializable
    class PipelineRunsResponse(
        override val payload: List<PipelineRun>,
    ): Success<List<PipelineRun>> {
        @SerialName("object")
        override val responseObject: String = "pipeline_run"
    }

    /** API response for a list of records from [source_tables][SourceTables] */
    @Serializable
    class SourceTablesResponse(
        override val payload: List<SourceTable>,
    ): Success<List<SourceTable>> {
        @SerialName("object")
        override val responseObject: String = "source_table"
    }

    /** API response for a list of records from [source_tables][SourceTables] */
    @Serializable
    class SourceTableResponse(
        override val payload: SourceTable,
    ): Success<SourceTable> {
        @SerialName("object")
        override val responseObject: String = "source_table"
    }

    /** API response for a list of records from [internal_users][InternalUsers] */
    @Serializable
    class UsersResponse(
        override val payload: List<ResponseUser>,
    ): Success<List<ResponseUser>> {
        @SerialName("object")
        override val responseObject: String = "internal_user"
    }

    /** API response for a list of records from [internal_users][InternalUsers] */
    @Serializable
    class UserResponse(
        override val payload: RequestUser,
    ): Success<RequestUser> {
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
        override val payload: NextTask,
    ): Success<NextTask> {
        @SerialName("object")
        override val responseObject: String = "next_task"
    }

    /** API response for a list of records from [plotting_fields][PlottingFields] */
    @Serializable
    class PlottingFieldsResponse(
        override val payload: List<PlottingFieldBody>,
    ): Success<List<PlottingFieldBody>> {
        @SerialName("object")
        override val responseObject: String = "plotting_fields"
    }

    /** API response for a single record from [plotting_fields][PlottingFields] */
    @Serializable
    class PlottingFieldsSingle(
        override val payload: PlottingFieldBody,
    ): Success<PlottingFieldBody> {
        @SerialName("object")
        override val responseObject: String = "plotting_fields"
    }

    /** API response for a list of records from [plotting_methods][PlottingMethods] */
    @Serializable
    class PlottingMethodsResponse(
        override val payload: List<PlottingMethod>,
    ): Success<List<PlottingMethod>> {
        @SerialName("object")
        override val responseObject: String = "plotting_method"
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
