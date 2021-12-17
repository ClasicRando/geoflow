package me.geoflow.core.database.tables

import me.geoflow.core.database.extensions.runUpdate
import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.tables.records.PipelineTask
import me.geoflow.core.utils.requireNotEmpty
import java.sql.Connection

/**
 * Table used to store the tasks associated with a generic pipeline from the [Pipelines] table
 */
object PipelineTasks : DbTable("pipeline_tasks"), DefaultGeneratedData, Triggers {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.pipeline_tasks
        (
			pipeline_task_id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            pipeline_id bigint NOT NULL REFERENCES public.pipelines (pipeline_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
            task_id bigint NOT NULL REFERENCES public.tasks (task_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            task_order integer NOT NULL,
            CONSTRAINT pipeline_order UNIQUE (pipeline_id, task_order)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val dataGenerationSqlFile: String = "pipeline_tasks.sql"

    override val triggers: List<Trigger> = listOf(
        Trigger(
            trigger = """
                CREATE TRIGGER set_records_trigger
                    AFTER INSERT
                    ON public.pipeline_tasks
                    REFERENCING NEW TABLE AS new_table
                    FOR EACH STATEMENT
                    EXECUTE FUNCTION public.pipeline_tasks_inserts();
            """.trimIndent(),
            triggerFunction = """
                CREATE OR REPLACE FUNCTION public.pipeline_tasks_inserts()
                    RETURNS trigger
                    LANGUAGE 'plpgsql'
                    COST 100
                    VOLATILE NOT LEAKPROOF
                AS ${'$'}BODY${'$'}
                declare
                    check_count bigint;
                begin
                    select count(distinct pipeline_id)
                    into   check_count
                    from   new_table;
                    
                    if check_count > 1 then
                        raise exception 'Cannot insert for multiple pipeline_id';
                    end if;
                    
                    select count(0)
                    into   check_count
                    from  (select task_order, row_number() over (order by task_order) rn
                           from   new_table) t1
                    where  rn != task_order;
                    
                    if check_count > 0 then
                        raise exception 'The order of the task order has skipped a value';
                    end if;
                    return null;
                end;
                ${'$'}BODY${'$'};
            """.trimIndent(),
        ),
    )

    /** Returns a list of all pipeline tasks for the given [pipelineId] */
    fun getRecords(connection: Connection, pipelineId: Long): List<PipelineTask> {
        return connection.submitQuery(sql = "SELECT * FROM $tableName WHERE pipeline_id = ?", pipelineId)
    }

    /**
     * Attempts to set the pipeline tasks for a given [pipelineId] using the list of [pipelineTasks] provided. Performs
     * a complete replacement of current pipeline tasks. Returns delete and insert counts as a [Pair].
     *
     * This method should always be called from an open transaction (i.e. connection without autocommit) to avoid
     * deleting records without a rollback available if the new records do not pass the checks/assertions.
     *
     * @throws IllegalStateException when [pipelineTasks] is empty
     * @throws me.geoflow.core.database.errors.IllegalUserAction when the user does not have the privileges required
     * @throws java.sql.SQLException when the connection throws an exception. Usually means a constraint or trigger
     * failed
     */
    fun setRecords(
        connection: Connection,
        userId: Long,
        pipelineId: Long,
        pipelineTasks: List<PipelineTask>,
    ): Pair<Int, Int> {
        requireNotEmpty(pipelineTasks) { "Pipeline tasks provided must be a non-empty list" }
        InternalUsers.requireRole(connection, userId, "pipeline_edit")
        val deleteCount = connection.runUpdate(
            sql = "DELETE FROM $tableName WHERE pipeline_id = ?",
            pipelineId
        )
        val insertCount = connection.runUpdate(
            sql = """
                INSERT INTO $tableName(pipeline_id,task_id,task_order)
                VALUES${"(?,?,?,?),".repeat(pipelineTasks.size).trimEnd(',')}
            """.trimIndent(),
            pipelineTasks.flatMap { listOf(it.pipelineId, it.taskId, it.taskOrder) },
        )
        return deleteCount to insertCount
    }

}
