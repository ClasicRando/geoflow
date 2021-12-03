package database.tables

import database.NoRecordFound
import database.enums.MergeType
import database.enums.OperationState
import database.extensions.queryFirstOrNull
import database.extensions.queryHasResult
import database.extensions.runUpdate
import database.extensions.submitQuery
import formatLocalDateDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.sql.Connection
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Table used to store runs of generic pipelines. Holds details about the run while linking to the backing data source
 * to contain reference to what data is to be loaded.
 */
object PipelineRuns : DbTable("pipeline_runs"), ApiExposed, Triggers {

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "ds_id" to mapOf("name" to "Data Source ID"),
        "ds_code" to mapOf("name" to "Data Source Code"),
        "record_date" to mapOf(),
        "operation_state" to mapOf(),
        "collection_user" to mapOf(),
        "load_user" to mapOf(),
        "check_user" to mapOf(),
        "qa_user" to mapOf("name" to "QA User"),
        "actions" to mapOf("formatter" to "actionsFormatter")
    )

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.pipeline_runs
        (
            run_id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            ds_id bigint NOT NULL REFERENCES public.data_sources (ds_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
            record_date date NOT NULL,
            collection_user_oid bigint REFERENCES public.internal_users (user_oid) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE SET NULL,
            load_user_oid bigint REFERENCES public.internal_users (user_oid) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE SET NULL,
            check_user_oid bigint REFERENCES public.internal_users (user_oid) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE SET NULL,
            qa_user_oid bigint REFERENCES public.internal_users (user_oid) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE SET NULL,
            workflow_operation text COLLATE pg_catalog."default" NOT NULL
                REFERENCES public.workflow_operations (code) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            operation_state operation_state NOT NULL,
            production_count integer NOT NULL,
            staging_count integer NOT NULL,
            match_count integer NOT NULL,
            new_count integer NOT NULL,
            plotting_stats jsonb NOT NULL,
            has_child_tables boolean NOT NULL,
            merge_type merge_type
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val triggers: List<Trigger> = listOf(
        // Trigger function for updating a pipeline run record
        Trigger(
            trigger = """
                CREATE TRIGGER update_record
                    BEFORE UPDATE 
                    ON public.pipeline_runs
                    FOR EACH ROW
                    EXECUTE FUNCTION public.update_pipeline_run();
            """.trimIndent(),
            triggerFunction = """
                CREATE OR REPLACE FUNCTION public.update_pipeline_run()
                    RETURNS trigger
                    LANGUAGE 'plpgsql'
                    COST 100
                    VOLATILE NOT LEAKPROOF
                AS ${'$'}BODY${'$'}
                DECLARE
                    v_pipeline_id BIGINT;
                BEGIN
                    SELECT CASE NEW.workflow_operation
                            WHEN 'collection' THEN collection_pipeline
                            WHEN 'load' THEN load_pipeline
                            WHEN 'check' THEN check_pipeline
                            WHEN 'qa' THEN qa_pipeline
                           END
                    INTO   v_pipeline_id
                    FROM   data_sources
                    WHERE  ds_id = NEW.ds_id;
                    IF NEW.operation_state = 'Active' AND OLD.operation_state = 'Ready' THEN
                        INSERT INTO pipeline_run_tasks(run_id,task_id,parent_task_id,parent_task_order)
                        SELECT NEW.run_id, t2.task_id, t2.parent_task, t2.parent_task_order
                        FROM   pipelines t1
                        JOIN   pipeline_tasks t2
                        ON	   t1.pipeline_id = t2.pipeline_id
                        WHERE  t1.pipeline_id = v_pipeline_id;
                    END IF;
                    RETURN NEW;
                END;
                ${'$'}BODY${'$'};
            """.trimIndent(),
        ),
    )

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
                FROM   $tableName t1
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

    /**
     * Returns the runs associated with a userId and within a specified state
     *
     * Future Changes
     * --------------
     * - should make the state value an enum to avoid unwanted state
     *
     * @throws IllegalArgumentException when the state provided does not match the required values
     */
    fun userRuns(connection: Connection, userId: Long, state: String): List<PipelineRun> {
        val isAdmin = connection.queryHasResult(
            sql = "SELECT 1 FROM ${InternalUsers.tableName} WHERE user_oid = ? AND 'admin' = ANY(roles)",
            userId,
        )
        require(state in listOf("collection", "load", "check", "qa")) {
            "state provided does not point to anything"
        }
        return if (isAdmin) {
            connection.submitQuery(
                sql = "${PipelineRun.sql} WHERE workflow_operation = ?",
                state,
            )
        } else {
            connection.submitQuery(
                sql = """
                    WITH user_operations AS (
                        SELECT code
                        FROM   ${WorkflowOperations.tableName} wo
                        JOIN   ${InternalUsers.tableName} iu
                        ON     wo.role = ANY(iu.roles)
                        WHERE  iu.user_oid = ?
                    )
                    ${PipelineRun.sql}
                    WHERE workflow_operation = ?
                    AND   COALESCE(t1.${state}_user_oid, ?) = ?
                    AND   ? IN (SELECT code FROM user_operations)"
                """.trimIndent(),
                userId,
                state,
                userId,
                userId,
                state,
            )
        }
    }

    /**
     * Returns the last runId for the data source linked to the pipeline run task. If a past run cannot be found, null
     * is returned
     */
    fun lastRun(connection: Connection, pipelineRunTaskId: Long): Long? {
        val sql = """
            SELECT t3.ds_id
            FROM   $tableName t1
            JOIN   ${PipelineRunTasks.tableName} t2
            ON     t1.run_id = t2.run_id
            LEFT JOIN $tableName t3
            ON     t1.ds_id = t3.ds_id
            WHERE  t2.pr_task_id = ?
            ORDER BY 1 DESC
            LIMIT  1 OFFSET 1
        """.trimIndent()
        return connection.queryFirstOrNull(sql = sql, pipelineRunTaskId)
    }

    /**
     * Attempts to return a PipelineRun entity from the provided runId. Returns null if the runId does not match any
     * records
     */
    fun getRun(connection: Connection, runId: Long): PipelineRun? {
        return connection.queryFirstOrNull(
            sql = "${PipelineRun.sql} WHERE t1.run_id = ?",
            runId,
        )
    }

    /**
     * Sets a run to a specific user for the current workflow operation.
     *
     * @throws NoRecordFound when the runId provided does not link to a record
     */
    fun pickupRun(connection: Connection, runId: Long, userId: Long) {
        val workflowSql = """
            SELECT workflow_operation
            FROM   $tableName
            WHERE  run_id = ?
            AND    operation_state = 'Ready'::operation_state
            FOR UPDATE
        """.trimIndent()
        val workflowOperation = connection.queryFirstOrNull<String>(
            sql = workflowSql,
            runId
        ) ?: throw NoRecordFound(
            tableName = tableName,
            message = "RunId provided could does not link to a record or run is not 'Ready'",
        )
        val updateSql = """
            UPDATE $tableName
            SET    operation_state = ?,
                   ${workflowOperation}_user_oid = ?
            WHERE  run_id = ?
        """.trimIndent()
        connection.runUpdate(
            sql = updateSql,
            OperationState.Active.pgObject,
            userId,
            runId,
        )
    }
}
