package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.geoflow.core.database.enums.MergeType
import me.geoflow.core.database.enums.OperationState
import me.geoflow.core.database.tables.DataSources
import me.geoflow.core.database.tables.InternalUsers
import me.geoflow.core.database.tables.PipelineRuns
import me.geoflow.core.utils.formatLocalDateDefault
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** API response data class for JSON serialization */
@Suppress("UNUSED")
@Serializable
data class PipelineRun(
    /** unique ID of the pipeline run */
    @SerialName("run_id")
    val runId: Long,
    /** ID of the data source this run is owned by */
    @SerialName("ds_id")
    val dsId: Long,
    /** reference code of the data source */
    @SerialName("ds_code")
    val dsCode: String,
    /** base location of data source files */
    @SerialName("files_location")
    val filesLocation: String,
    /** Record date of the dataset used for loading. Represents the max record date if varying date's per file */
    @SerialName("record_date")
    val recordDate: String,
    /** current workflow operation of the run */
    @SerialName("workflow_operation")
    val workflowOperation: String,
    /** Current state of the run within the given workflow operation. Gets enum value using name */
    @SerialName("operation_state")
    val operationState: String,
    /** Name of the user that performed or is performing the collection. Null if not currently known/assigned */
    @SerialName("collection_user")
    val collectionUser: String?,
    /** Name of the user that performed or is performing the load. Null if not currently known/assigned */
    @SerialName("load_user")
    val loadUser: String?,
    /** Name of the user that performed or is performing the check. Null if not currently known/assigned */
    @SerialName("check_user")
    val checkUser: String?,
    /** Name of the user that performed or is performing the qa. Null if not currently known/assigned */
    @SerialName("qa_user")
    val qaUser: String?,
    /** Number of records found in the main production data */
    @SerialName("production_count")
    val productionCount: Int,
    /** Number of records found in the main staging data */
    @SerialName("staging_count")
    val stagingCount: Int,
    /** Number of records matched to production's main data */
    @SerialName("match_count")
    val matchCount: Int,
    /** Number of records not matched to production's main data */
    @SerialName("new_count")
    val newCount: Int,
    /** jsonb fields converted to [Map]. Describes the plotting statistics of the staging data */
    @SerialName("plotting_stats")
    val plottingStats: String,
    /** Flag denoting if the run has child details */
    @SerialName("has_child_tables")
    val hasChildTables: Boolean,
    /** Current merge state of the run. Gets enum value using name */
    @SerialName("merge_type")
    val mergeType: String,
) {
    /** [LocalDate] representation of [recordDate] */
    private val recordLocalDate: LocalDate get() = LocalDate.parse(recordDate, DateTimeFormatter.ISO_LOCAL_DATE)
    /** Generated file location for the current pipeline run */
    val runFilesLocation: String get() = "$filesLocation/${formatLocalDateDefault(recordLocalDate)}/files"
    /** Generated zip location for the current pipeline run */
    val runZipLocation: String get() = "$filesLocation/${formatLocalDateDefault(recordLocalDate)}/zip"
    /** Generated zip file name for the current pipeline run */
    val backupZip: String get() = "${dsCode}_${formatLocalDateDefault(recordLocalDate)}"
    /** Current merge state of the run. Gets enum value using name */
    val mergeTypeEnum: MergeType get() = MergeType.valueOf(mergeType)
    /** Current state of the run within the given workflow operation. Gets enum value using name */
    val operationStateEnum: OperationState get() = OperationState.valueOf(operationState)
    /** plotting stats converted to a [Map] of plotting type and number of records with that type */
    val plottingStatsJson: Map<String, Int> get() = Json.decodeFromString(plottingStats)

    init {
        require(runCatching { MergeType.valueOf(mergeType) }.isSuccess) {
            "string value passed for MergeType is not valid"
        }
        require(runCatching { OperationState.valueOf(operationState) }.isSuccess) {
            "string value passed for OperationState is not valid"
        }
        require(runCatching { Json.decodeFromString<Map<String, Int>>(plottingStats) }.isSuccess) {
            "string value passed for plottingStats could not be decoded to a Map<String, Int>"
        }
    }

    companion object {
        /** SQL query used to generate the parent class */
        val sql: String = """
                SELECT t1.run_id, t1.ds_id, t2.code, t2.files_location,
                       to_char(t1.record_date,'YYYY-MM-DD') record_date, t1.workflow_operation,
                       t1.operation_state, t3.name, t4.name, t5.name, t6.name, t1.production_count, t1.staging_count,
                       t1.match_count, t1.new_count, t1.plotting_stats, t1.has_child_tables, t1.merge_type
                FROM   ${PipelineRuns.tableName} t1
                JOIN   ${DataSources.tableName} t2
                ON     t1.ds_id = t2.ds_id
                LEFT JOIN ${InternalUsers.tableName} t3
                ON     t1.collection_user_oid = t3.user_oid
                LEFT JOIN ${InternalUsers.tableName} t4
                ON     t1.load_user_oid = t4.user_oid
                LEFT JOIN ${InternalUsers.tableName} t5
                ON     t1.check_user_oid = t5.user_oid
                LEFT JOIN ${InternalUsers.tableName} t6
                ON     t1.qa_user_oid = t6.user_oid
            """.trimIndent()
    }
}
