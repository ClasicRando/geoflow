package database.procedures

import java.sql.Connection
import kotlin.reflect.typeOf

/**
 * Stored Procedure that deletes all child tasks of a given pr_task_id. Uses recursive CTE to obtain all levels of child
 * tasks for deletion
 */
object DeleteRunTaskChildren: SqlProcedure(
    "delete_run_task_children",
    parameterTypes = listOf(
        typeOf<Long>(),
    ),
) {

    /** Call the stored procedure */
    fun call(connection: Connection, pipelineRunTaskId: Long) {
        super.call(connection, pipelineRunTaskId)
    }

    override val code = """
        CREATE OR REPLACE PROCEDURE public.delete_run_task_children(
        	pr_task_id bigint)
        LANGUAGE 'sql'
        AS         ${'$'}BODY${'$'}
        with recursive children as (
        	select pr_task_id
        	from   pipeline_run_tasks
        	where  parent_task_id = $1
        	union all
        	select t1.pr_task_id
        	from   pipeline_run_tasks t1, children t2
        	where  t1.parent_task_id = t2.pr_task_id
        )
        delete from pipeline_run_tasks t1
        using  children t2
        where  t1.pr_task_id = t2.pr_task_id;
        ${'$'}BODY${'$'};
    """.trimIndent()
}