package orm.tables

import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long
import orm.entities.PipelineTask

object PipelineTasks: Table<PipelineTask>("pipeline_tasks") {
    val pipelineId = long("pipeline_id").primaryKey().bindTo { it.pipelineId }
    val taskId = long("task_id").primaryKey().bindTo { it.taskId }
    val parentTask = long("parent_task").bindTo { it.parentTask }
    val parentTaskOrder = int("parent_task_order").bindTo { it.parentTaskOrder }

    val createStatement = """
        CREATE TABLE IF NOT EXISTS public.pipeline_tasks
        (
            pipeline_id bigint NOT NULL,
            task_id bigint NOT NULL,
            CONSTRAINT pipeline_tasks_pkey PRIMARY KEY (pipeline_id, task_id),
            CONSTRAINT pipeline_id FOREIGN KEY (pipeline_id)
                REFERENCES public.pipelines (pipeline_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE
                NOT VALID
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}