package me.geoflow.api.utils

import me.geoflow.core.database.tables.Actions
import me.geoflow.core.database.tables.DataSourceContacts
import me.geoflow.core.database.tables.DataSources
import me.geoflow.core.database.tables.InternalUsers
import me.geoflow.core.database.tables.GeneratedTableColumns
import me.geoflow.core.database.tables.Pipelines
import me.geoflow.core.database.tables.PipelineRelationships
import me.geoflow.core.database.tables.PipelineRelationshipFields
import me.geoflow.core.database.tables.PipelineRuns
import me.geoflow.core.database.tables.PipelineTasks
import me.geoflow.core.database.tables.PlottingFields
import me.geoflow.core.database.tables.PlottingMethods
import me.geoflow.core.database.tables.PlottingMethodTypes
import me.geoflow.core.database.tables.Provs
import me.geoflow.core.database.tables.RecordWarehouseTypes
import me.geoflow.core.database.tables.Roles
import me.geoflow.core.database.tables.SourceTables
import me.geoflow.core.database.tables.SourceTableColumns
import me.geoflow.core.database.tables.WorkflowOperations
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.geoflow.core.database.tables.records.Action
import me.geoflow.core.database.tables.records.CollectionUser
import me.geoflow.core.database.tables.records.ColumnComparison
import me.geoflow.core.database.tables.records.DataSource
import me.geoflow.core.database.tables.records.DataSourceContact
import me.geoflow.core.database.tables.records.GeneratedTableColumn
import me.geoflow.core.database.tables.records.LogicField
import me.geoflow.core.database.tables.records.NextTask
import me.geoflow.core.database.tables.records.Pipeline
import me.geoflow.core.database.tables.records.PipelineRelationship
import me.geoflow.core.database.tables.records.PipelineRelationshipField
import me.geoflow.core.database.tables.records.PipelineRun
import me.geoflow.core.database.tables.records.PipelineTask
import me.geoflow.core.database.tables.records.PlottingFieldBody
import me.geoflow.core.database.tables.records.PlottingMethod
import me.geoflow.core.database.tables.records.PlottingMethodType
import me.geoflow.core.database.tables.records.Prov
import me.geoflow.core.database.tables.records.RecordWarehouseType
import me.geoflow.core.database.tables.records.RequestUser
import me.geoflow.core.database.tables.records.ResponseUser
import me.geoflow.core.database.tables.records.Role
import me.geoflow.core.database.tables.records.SourceTable
import me.geoflow.core.database.tables.records.SourceTableColumn
import me.geoflow.core.database.tables.records.TableCountComparison
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

    /** API response for a list of records from [generated_table_columns][GeneratedTableColumns] */
    @Serializable
    class GeneratedTableColumnsResponse(
        override val payload: List<GeneratedTableColumn>,
    ): Success<List<GeneratedTableColumn>> {
        @SerialName("object")
        override val responseObject: String = "generated_table_column"
    }

    /** API response for a single record from [generated_table_columns][GeneratedTableColumns] */
    @Serializable
    class GeneratedTableColumnResponse(
        override val payload: GeneratedTableColumn,
    ): Success<GeneratedTableColumn> {
        @SerialName("object")
        override val responseObject: String = "generated_table_column"
    }

    /** API response for a list of records from [source_table_columns][SourceTableColumns] */
    @Serializable
    class SourceTableColumnsResponse(
        override val payload: List<SourceTableColumn>,
    ): Success<List<SourceTableColumn>> {
        @SerialName("object")
        override val responseObject: String = "source_table_column"
    }

    /** API response for a single record from [source_table_columns][SourceTableColumns] */
    @Serializable
    class SourceTableColumnResponse(
        override val payload: SourceTableColumn,
    ): Success<SourceTableColumn> {
        @SerialName("object")
        override val responseObject: String = "source_table_column"
    }

    /** API response for a list of fields used to generate loading logic (Union of source and generated columns */
    @Serializable
    class LogicFieldsResponse(
        override val payload: List<LogicField>,
    ): Success<List<LogicField>> {
        @SerialName("object")
        override val responseObject: String = "logic_field"
    }

    /** API response for a list of records from [source_tables][SourceTables] */
    @Serializable
    class SourceTableResponse(
        override val payload: SourceTable,
    ): Success<SourceTable> {
        @SerialName("object")
        override val responseObject: String = "source_table"
    }

    /** API response for a list of records from [pipeline_relationships][PipelineRelationships] */
    @Serializable
    class PipelineRelationshipsResponse(
        override val payload: List<PipelineRelationship>,
    ): Success<List<PipelineRelationship>> {
        @SerialName("object")
        override val responseObject: String = "pipeline_relationship"
    }

    /** API response for a list of records from [pipeline_relationship_fields][PipelineRelationshipFields] */
    @Serializable
    class PipelineRelationshipFieldsResponse(
        override val payload: List<PipelineRelationshipField>,
    ): Success<List<PipelineRelationshipField>> {
        @SerialName("object")
        override val responseObject: String = "pipeline_relationship_field"
    }

    /** API response for a list of table count comparison records */
    @Serializable
    class TableCountComparisonsResponse(
        override val payload: List<TableCountComparison>,
    ): Success<List<TableCountComparison>> {
        @SerialName("object")
        override val responseObject: String = "table_count_comparison"
    }

    /** API response for a list of column comparison records */
    @Serializable
    class ColumnComparisonsResponse(
        override val payload: List<ColumnComparison>,
    ): Success<List<ColumnComparison>> {
        @SerialName("object")
        override val responseObject: String = "column_comparison"
    }

    /** API response for a list of records from [internal_users][InternalUsers] */
    @Serializable
    class UsersResponse(
        override val payload: List<ResponseUser>,
    ): Success<List<ResponseUser>> {
        @SerialName("object")
        override val responseObject: String = "internal_user"
    }

    /** API response for a list of records from [internal_users][InternalUsers] with the collection role */
    @Serializable
    class CollectionUsersResponse(
        override val payload: List<CollectionUser>,
    ): Success<List<CollectionUser>> {
        @SerialName("object")
        override val responseObject: String = "collection_user"
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

    /** API response for a list of records from [plotting_method_types][PlottingMethodTypes] */
    @Serializable
    class PlottingMethodTypesResponse(
        override val payload: List<PlottingMethodType>,
    ): Success<List<PlottingMethodType>> {
        @SerialName("object")
        override val responseObject: String = "plotting_method_type"
    }

    /** API response for a list of records from [data_sources][DataSources] */
    @Serializable
    class DataSourcesResponse(
        override val payload: List<DataSource>,
    ): Success<List<DataSource>> {
        @SerialName("object")
        override val responseObject: String = "data_source"
    }

    /** API response for a single record from [data_sources][DataSources] */
    @Serializable
    class DataSourceResponse(
        override val payload: DataSource,
    ): Success<DataSource> {
        @SerialName("object")
        override val responseObject: String = "data_source"
    }

    /** API response for a list of records from [data_source_contacts][DataSourceContacts] */
    @Serializable
    class DataSourceContactsResponse(
        override val payload: List<DataSourceContact>,
    ): Success<List<DataSourceContact>> {
        @SerialName("object")
        override val responseObject: String = "data_source_contact"
    }

    /** API response for a single record from [data_source_contacts][DataSourceContacts] */
    @Serializable
    class DataSourceContactResponse(
        override val payload: DataSourceContact,
    ): Success<DataSourceContact> {
        @SerialName("object")
        override val responseObject: String = "data_source_contact"
    }

    /** API response for a list of records from [provs][Provs] */
    @Serializable
    class ProvsResponse(
        override val payload: List<Prov>,
    ): Success<List<Prov>> {
        @SerialName("object")
        override val responseObject: String = "prov"
    }

    /** API response for a list of records from [roles][Roles] */
    @Serializable
    class RolesResponse(
        override val payload: List<Role>,
    ): Success<List<Role>> {
        @SerialName("object")
        override val responseObject: String = "role"
    }

    /** API response for a list of records from [record_warehouse_types][RecordWarehouseTypes] */
    @Serializable
    class RecordWarehouseTypesResponse(
        override val payload: List<RecordWarehouseType>,
    ): Success<List<RecordWarehouseType>> {
        @SerialName("object")
        override val responseObject: String = "record_warehouse_type"
    }

    /** API response for a single record from [pipelines][Pipelines] */
    @Serializable
    class PipelineResponse(
        override val payload: Pipeline,
    ): Success<Pipeline> {
        @SerialName("object")
        override val responseObject: String = "pipeline"
    }

    /** API response for a list of records from [pipelines][Pipelines] */
    @Serializable
    class PipelinesResponse(
        override val payload: List<Pipeline>,
    ): Success<List<Pipeline>> {
        @SerialName("object")
        override val responseObject: String = "pipeline"
    }

    /** API response for a list of records from [pipeline_tasks][PipelineTasks] */
    @Serializable
    class PipelineTasksResponse(
        override val payload: List<PipelineTask>,
    ): Success<List<PipelineTask>> {
        @SerialName("object")
        override val responseObject: String = "pipeline_task"
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
