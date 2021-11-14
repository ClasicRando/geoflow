package database.tables

/**
 * Table used to store the tasks associated with a generic pipeline from the [Pipelines] table
 */
object PipelineTasks: DbTable("pipeline_tasks") {

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.pipeline_tasks
        (
			pipeline_task_id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            pipeline_id bigint NOT NULL REFERENCES public.pipelines (pipeline_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
            task_id bigint NOT NULL REFERENCES public.tasks (task_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            parent_task bigint NOT NULL DEFAULT 0,
            parent_task_order integer NOT NULL,
            CONSTRAINT unique_parent_order UNIQUE (pipeline_id, parent_task, parent_task_order)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}
