package database.tables

import database.enums.MergeType
import database.enums.OperationState
import database.extensions.queryFirstOrNull
import database.extensions.runUpdate
import database.extensions.submitQuery
import formatLocalDateDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.postgresql.util.PGobject
import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate

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

    /** Table record for [PipelineRuns] */
    @Suppress("LongParameterList", "UNUSED")
    @TableRecord
    class PipelineRun private constructor(
        /** unique ID of the pipeline run */
        val runId: Long,
        /** ID of the data source this run is owned by */
        val dsId: Long,
        /** reference code of the data source */
        val dsCode: String,
        filesLocation: String,
        recordDate: Date,
        /** current workflow operation of the run */
        val workflowOperation: String,
        operationState: String,
        /** ID of the user that performed or is performing the collection. Null if not currently known/assigned */
        val collectionUserOid: Long?,
        /** ID of the user that performed or is performing the load. Null if not currently known/assigned */
        val loadUserOid: Long?,
        /** ID of the user that performed or is performing the check. Null if not currently known/assigned */
        val checkUserOid: Long?,
        /** ID of the user that performed or is performing the qa. Null if not currently known/assigned */
        val qaUserOid: Long?,
        /** Number of records found in the main production data */
        val productionCount: Int,
        /** Number of records found in the main staging data */
        val stagingCount: Int,
        /** Number of records matched to production's main data */
        val matchCount: Int,
        /** Number of records not matched to production's main data */
        val newCount: Int,
        plottingStats: PGobject,
        /** Flag denoting if the run has child details */
        val hasChildTables: Boolean,
        mergeType: String,
    ) {
        /** Current state of the run within the given workflow operation. Gets enum value using name */
        val operationState: OperationState = OperationState.valueOf(operationState)
        /** Record date of the dataset used for loading. Represents the max record date if varying date's per file */
        val recordDate: LocalDate = recordDate.toLocalDate()
        /** jsonb fields converted to [Map]. Describes the plotting statistics of the staging data */
        val plottingStats: Map<String, Int> = Json.decodeFromString(plottingStats.value ?: "")
        /** Current merge state of the run. Gets enum value using name */
        val mergeType: MergeType = MergeType.valueOf(mergeType)
        /** Generated file location for the current pipeline run */
        val runFilesLocation: String = "$filesLocation/${formatLocalDateDefault(this.recordDate)}/files"
        /** Generated zip location for the current pipeline run */
        val runZipLocation: String = "$filesLocation/${formatLocalDateDefault(this.recordDate)}/zip"
        /** Generated zip file name for the current pipeline run */
        val backupZip: String = "${dsCode}_${formatLocalDateDefault(this.recordDate)}"

        @Suppress("UNUSED")
        companion object {
            /** SQL query used to generate the parent class */
            val sql: String = """
                SELECT t1.run_id, t1.ds_id, t2.code, t2.files_location, t1.record_Date, t1.workflow_operation,
                       t1.operation_state, t1.collection_user_oid, t1.load_user_oid, t1.check_user_oid, t1.qa_user_oid,
                       t1.production_count, t1.staging_count, t1.match_count, t1.new_count, t1.plotting_stats,
                       t1.has_child_tables, t1.merge_type
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
                WHERE  t1.run_id = ?
            """.trimIndent()
            private const val RUN_ID = 1
            private const val DS_ID = 2
            private const val DS_CODE = 3
            private const val FILES_LOCATION = 4
            private const val RECORD_DATE = 5
            private const val WORKFLOW_OPERATION = 6
            private const val OPERATION_STATE = 7
            private const val COLLECTION_USER = 8
            private const val LOAD_USER = 9
            private const val CHECK_USER = 10
            private const val QA_USER = 11
            private const val PRODUCTION_COUNT = 12
            private const val STAGING_COUNT = 13
            private const val MATCH_COUNT = 14
            private const val NEW_COUNT = 15
            private const val PLOTTING_STATS = 16
            private const val HAS_CHILDREN_TABLES = 17
            private const val MERGE_TYPE = 18

            /** Function used to process a [ResultSet] into a Table record */
            fun fromResultSet(rs: ResultSet): PipelineRun {
                return PipelineRun(
                    runId = rs.getLong(RUN_ID),
                    dsId = rs.getLong(DS_ID),
                    dsCode = rs.getString(DS_CODE),
                    filesLocation = rs.getString(FILES_LOCATION),
                    recordDate = rs.getDate(RECORD_DATE),
                    workflowOperation = rs.getString(WORKFLOW_OPERATION),
                    operationState = rs.getString(OPERATION_STATE),
                    collectionUserOid = rs.getLong(COLLECTION_USER),
                    loadUserOid = rs.getLong(LOAD_USER),
                    checkUserOid = rs.getLong(CHECK_USER),
                    qaUserOid = rs.getLong(QA_USER),
                    productionCount = rs.getInt(PRODUCTION_COUNT),
                    stagingCount = rs.getInt(STAGING_COUNT),
                    matchCount = rs.getInt(MATCH_COUNT),
                    newCount = rs.getInt(NEW_COUNT),
                    plottingStats = rs.getObject(PLOTTING_STATS) as PGobject,
                    hasChildTables = rs.getBoolean(HAS_CHILDREN_TABLES),
                    mergeType = rs.getString(MERGE_TYPE),
                )
            }
        }
    }

    /**
     * Checks if a username is linked to the given runId. Returns true if user is a part of the run or the user has the
     * admin role.
     *
     * @throws IllegalArgumentException when the username does not link to a user in [InternalUsers]
     */
    fun checkUserRun(connection: Connection, runId: Long, username: String): Boolean {
        val sql = """
            WITH user_record as (
                SELECT *
                FROM   ${InternalUsers.tableName}
                WHERE  username = ?
            )
            SELECT 1
            FROM   $tableName t1
            LEFT JOIN user_record t2
            ON     t1.collection_user_oid = t2.user_oid
            LEFT JOIN user_record t3
            ON     t1.load_user_oid = t3.user_oid
            LEFT JOIN user_record t4
            ON     t1.check_user_oid = t4.user_oid
            LEFT JOIN user_record t5
            ON     t1.qa_user_oid = t5.user_oid
            LEFT JOIN user_record t6
            ON     'admin' = ANY(t6.roles)
            WHERE  t1.run_id = ?
            AND    COALESCE(t2.user_oid,t3.user_oid,t4.user_oid,t5.user_oid,t6.user_oid) IS NOT NULL
        """.trimIndent()
        return connection.prepareStatement(sql).use { statement ->
            statement.setString(1, username)
            statement.setLong(2, runId)
            statement.executeQuery().use { rs ->
                rs.next()
            }
        }
    }

    /** API response data class for JSON serialization */
    @Serializable
    data class Record(
        /** unique ID of the pipeline run */
        @SerialName("run_id")
        val runId: Long,
        /** ID of the data source this run is owned by */
        @SerialName("ds_id")
        val dsId: Long,
        /** reference code of the data source */
        @SerialName("ds_code")
        val dsCode: String,
        /** Record date of the dataset used for loading. Represents the max record date if varying date's per file */
        @SerialName("record_date")
        val recordDate: String,
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
    )

    /**
     * Returns the runs associated with a userId and within a specified state
     *
     * Future Changes
     * --------------
     * - should make the state value an enum to avoid unwanted state
     *
     * @throws IllegalArgumentException when the state provided does not match the required values
     */
    fun userRuns(connection: Connection, userId: Long, state: String): List<Record> {
        require(state in listOf("collection", "load", "check", "qa")) {
            "state provided does not point to anything"
        }
        val sql = """
            SELECT t1.run_id, t1.ds_id, t2.code, to_char(t1.record_date,'YYYY-MM-DD') record_date, t1.operation_state,
                   t3.name, t4.name, t5.name, t6.name
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
            WHERE  COALESCE(t1.${state}_user_oid, ?) = ?
            AND    workflow_operation = ?
        """.trimIndent()
        return connection.submitQuery(sql = sql, userId, userId, state)
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
        return connection.queryFirstOrNull(sql = PipelineRun.sql, runId)
    }

    /**
     * Sets a run to a specific user for the current workflow operation.
     *
     * @throws IllegalArgumentException when the runId provided does not link to a record
     */
    fun pickupRun(connection: Connection, runId: Long, userId: Long) {
        val workflowSql = "SELECT workflow_operation FROM $tableName WHERE run_id = ? FOR UPDATE"
        val workflowOperation = connection.queryFirstOrNull<String>(
            sql = workflowSql,
            runId
        ) ?: throw IllegalArgumentException("RunId provided could does not link to a record")
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
