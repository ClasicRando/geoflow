package me.geoflow.core.database.tables

/** Table to store reworked or deleted tasks */
@Suppress("unused")
object PipelineRunTasksRemoved : DbTable("pipeline_run_tasks_removed") {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.pipeline_run_tasks_removed
        (
            pr_task_id bigint NOT NULL,
            run_id bigint NOT NULL,
            task_start timestamp with time zone,
            task_completed timestamp with time zone,
            task_id bigint NOT NULL,
            task_message text COLLATE pg_catalog."default",
            task_status task_status NOT NULL DEFAULT 'Waiting'::task_status,
            parent_task_id bigint NOT NULL DEFAULT 0,
            parent_task_order integer NOT NULL,
            workflow_operation text COLLATE pg_catalog."default" NOT NULL,
            task_stack_trace text COLLATE pg_catalog."default",
            modal_html text COLLATE pg_catalog."default",
            CONSTRAINT pipeline_run_tasks_removed_pkey1 PRIMARY KEY (pr_task_id)
        )
        WITH (
            OIDS = FALSE
        )
    """.trimIndent()
}
