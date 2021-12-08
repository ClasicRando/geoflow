package me.geoflow.core.database.functions

import me.geoflow.core.database.tables.PipelineRunTasks
import java.sql.Connection
import kotlin.reflect.full.withNullability
import kotlin.reflect.typeOf

/**
 * Table Function to retrieve all tasks belonging to a pipeline run, ordered by relative parent child order.
 *
 * This means that the function will start with parentId of 0, and select each task followed by the children of that
 * task if they exist. For example, if a run has the following tasks (structured as 'Task ID', 'Parent Task ID',
 * 'Parent Task Order'):
 *
 * - A, 0, 1
 * - B, 0, 2
 * - C, 0, 3
 * - D, B, 1
 * - E, B, 2
 * - F, D, 1
 *
 * The resulting list of tasks would be (by 'Task ID' or first column):
 *
 * 1. A
 * 2. B
 * 3. D
 * 4. F
 * 5. E
 * 6. C
 */
object GetTasksOrdered : PlPgSqlTableFunction(
    name = "get_tasks_ordered",
    parameterTypes = listOf(
        typeOf<Long>(),
        typeOf<String>().withNullability(true),
    ),
) {

    /**
     * Current scope 'call' function that uses the [runId] and [workflowState] to call the parent class scope 'call'
     * function. Makes sure users of this object provide the correct parameters before calling the super function.
     * Returns the list of ResultSet rows as a map
     */
    fun getTasks(connection: Connection, runId: Long, workflowState: String? = null): List<PipelineRunTasks.Record> {
        return call(connection, runId, workflowState)
    }

    override val functionCode: String = """
        CREATE OR REPLACE FUNCTION public.get_tasks_ordered(
            p_run_id bigint,
			workflow_operation text default null,
            OUT task_order bigint,
            OUT pr_task_id bigint,
            OUT run_id bigint,
            OUT task_start timestamp without time zone,
            OUT task_completed timestamp without time zone,
            OUT task_id bigint,
            OUT task_message text,
            OUT task_status task_status,
            OUT parent_task_id bigint,
            OUT parent_task_order integer,
            OUT workflow_operation text,
            OUT task_stack_trace text,
            OUT task_name text,
            OUT task_description text,
            OUT task_run_type task_run_type)
            RETURNS SETOF record 
            LANGUAGE 'sql'
            COST 100
            VOLATILE PARALLEL UNSAFE
            ROWS 1000
        
        AS ${'$'}BODY${'$'}
            with run_tasks as (
                select pr_task_id, row_number() over () task_order
                from   get_task_children($1,0)
            )
            select t1.task_order, t2.*, t3.name, t3.description, t3.task_run_type
            from   run_tasks t1
            join   pipeline_run_tasks t2
            on     t1.pr_task_id = t2.pr_task_id
            join   tasks t3
            on     t2.task_id = t3.task_id
			join   pipeline_runs t4
			on     t2.run_id = t4.run_id
			where  t2.workflow_operation = coalesce($2,t4.workflow_operation)
            order by 1;
        ${'$'}BODY${'$'};
    """.trimIndent()

    override val innerFunctions: List<String> = listOf(
        """
            CREATE OR REPLACE FUNCTION public.get_task_children(
                p_run_id bigint,
                p_parent_task_id bigint,
                OUT pr_task_id bigint)
                RETURNS SETOF bigint 
                LANGUAGE 'plpgsql'
                COST 100
                VOLATILE PARALLEL UNSAFE
                ROWS 1000
            
            AS ${'$'}BODY${'$'}
            declare
                r record;
                i record;
            begin
                for r in (select distinct t1.pr_task_id, t1.parent_task_order,
                                 case when t2.task_id is not null then true else false end has_children
                          from   pipeline_run_tasks t1
                          left join pipeline_run_tasks t2
                          on     t1.run_id = t2.run_id
                          and    t1.pr_task_id = t2.parent_task_id
                          left join tasks t3
                          on     t1.task_id = t3.task_id
                          where  t1.run_id = $1
                          and    t1.parent_task_id = $2
                          order by t1.parent_task_order)
                loop
                    pr_task_id := r.pr_task_id;
                    return next;
                    if r.has_children then
                        for i in (select * from get_task_children($1, r.pr_task_id))
                        loop
                            pr_task_id := i.pr_task_id;
                            return next;
                        end loop;
                    end if;
                end loop;
            end;
            ${'$'}BODY${'$'};
        """.trimIndent(),
    )
}
