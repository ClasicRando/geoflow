package database.functions

object GetTasksOrdered: PlPgSqlTableFunction(
    name = "gettasksordered",
    parameterTypes = listOf(Long::class)
) {

    val code = """
        CREATE OR REPLACE FUNCTION public.gettasksordered(
        	p_run_id bigint,
        	OUT pr_task_id bigint,
        	OUT run_id bigint,
        	OUT task_start timestamp without time zone,
        	OUT task_completed timestamp without time zone,
        	OUT task_id bigint,
        	OUT task_name text,
        	OUT task_status task_status,
        	OUT parent_task_id bigint,
        	OUT parent_task_order bigint)
        	RETURNS SETOF RECORD 
        	LANGUAGE 'sql'
        	COST 100
        	VOLATILE PARALLEL UNSAFE
        	ROWS 1000

        AS ${'$'}BODY${'$'}
        	select * from GetTaskChildren($1, 0);
        ${'$'}BODY${'$'};
    """.trimIndent()

    val innerFunction = """
        CREATE OR REPLACE FUNCTION public.gettaskchildren(
        	p_run_id bigint,
        	p_parent_task_id bigint,
        	OUT pr_task_id bigint,
        	OUT run_id bigint,
        	OUT task_start timestamp without time zone,
        	OUT task_completed timestamp without time zone,
        	OUT task_id bigint,
        	OUT task_name text,
        	OUT task_status task_status,
        	OUT parent_task_id bigint,
        	OUT parent_task_order bigint)
            RETURNS SETOF RECORD 
            LANGUAGE 'plpgsql'
            COST 100
            VOLATILE PARALLEL UNSAFE
            ROWS 1000

        AS ${'$'}BODY${'$'}
        declare
        	r record;
        	i record;
        begin
        	for r in (select distinct t1.*, t3.name, case when t2.task_id is not null then true else false end has_children
        			  from   pipeline_run_tasks t1
        			  left join pipeline_run_tasks t2
        			  on     t1.run_id = t2.run_id
        			  and    t1.task_id = t2.parent_task_id
        			  left join tasks t3
        			  on     t1.task_id = t3.task_id
        			  where  t1.run_id = $1
        			  and    t1.parent_task_id = $2
        			  order by t1.parent_task_order)
        	loop
        		select r.pr_task_id, r.run_id, r.task_start, r.task_completed, r.task_id, r.name, r.task_status, r.parent_task_id, r.parent_task_order
        		into   pr_task_id, run_id, task_start, task_completed, task_id, task_name, task_status, parent_task_id, parent_task_order;
        		return next;
        		if r.has_children then
        			for i in (select * from GetTaskChildren($1, r.task_id))
        			loop
        				select i.pr_task_id, i.run_id, i.task_start, i.task_completed, i.task_id, i.task_name, i.task_status, i.parent_task_id, i.parent_task_order
        				into   pr_task_id, run_id, task_start, task_completed, task_id, task_name, task_status, parent_task_id, parent_task_order;
        				return next;
        			end loop;
        		end if;
        	end loop;
        end;
        ${'$'}BODY${'$'};
    """.trimIndent()
}