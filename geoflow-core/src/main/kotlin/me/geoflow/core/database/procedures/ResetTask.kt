package me.geoflow.core.database.procedures

import java.sql.Connection
import kotlin.reflect.typeOf

/** */
object ResetTask : SqlProcedure(
    name = "rework_task",
    parameterTypes = listOf(
        typeOf<Long>(),
    ),
) {

    override val code: String = """
        CREATE OR REPLACE PROCEDURE public.rework_task(
        	p_pr_task_id bigint)
        LANGUAGE 'sql'
        AS ${'$'}BODY${'$'}
        WITH RECURSIVE children AS (
            SELECT pr_task_id
            FROM   pipeline_run_tasks
            WHERE  parent_task_id = $1
            UNION ALL
            SELECT t1.pr_task_id
            FROM   pipeline_run_tasks t1, children t2
            WHERE  t1.parent_task_id = t2.pr_task_id
        ), all_tasks AS (
            SELECT $1 pr_task_id
            UNION ALL
            SELECT pr_task_id
            FROM   children
        ), deleted_tasks AS (
            DELETE FROM pipeline_run_tasks t1
            USING  all_tasks t2
            WHERE  t1.pr_task_id = t2.pr_task_id
            RETURNING t1.pr_task_id
        ), removed_tasks AS (
            INSERT INTO pipeline_run_tasks_removed(
                pr_task_id,run_id,task_start,task_completed,task_id,task_message,task_status,parent_task_id,
                parent_task_order,workflow_operation,task_stack_trace,modal_html
            )
            SELECT dr.pr_task_id,dr.run_id,dr.task_start,dr.task_completed,dr.task_id,dr.task_message,dr.task_status,
                   dr.parent_task_id,dr.parent_task_order,dr.workflow_operation,dr.task_stack_trace,dr.modal_html
            FROM   pipeline_run_tasks dr
            JOIN   deleted_tasks t1
            ON     t1.pr_task_id = dr.pr_task_id
            RETURNING *
        )
        INSERT INTO pipeline_run_Tasks(run_id,task_status,task_id,parent_task_id,parent_task_order,workflow_operation)
        SELECT run_id, 'Waiting'::task_status, task_id, parent_task_id, parent_task_order, workflow_operation
        FROM   removed_tasks
        WHERE  pr_task_id = $1;
        ${'$'}BODY${'$'};
    """.trimIndent()

    /** */
    fun call(connection: Connection, prTaskId: Long) {
        super.call(connection, prTaskId)
    }

}
