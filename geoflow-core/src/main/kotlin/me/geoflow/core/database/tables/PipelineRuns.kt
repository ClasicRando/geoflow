package me.geoflow.core.database.tables

import me.geoflow.core.database.errors.NoRecordFound
import me.geoflow.core.database.enums.OperationState
import me.geoflow.core.database.extensions.queryFirstOrNull
import me.geoflow.core.database.extensions.queryHasResult
import me.geoflow.core.database.extensions.runUpdate
import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.tables.records.PipelineRun
import java.sql.Connection

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
                    AND   ? IN (SELECT code FROM user_operations)
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
