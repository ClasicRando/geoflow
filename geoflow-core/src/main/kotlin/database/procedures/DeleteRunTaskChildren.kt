package database.procedures

object DeleteRunTaskChildren: SqlProcedure(
    "delete_run_task_children",
    parameterTypes = listOf(Long::class)
) {
    val code = """
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