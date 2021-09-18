package orm.tables

import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long
import orm.entities.PipelineTask

object PipelineTasks: Table<PipelineTask>("pipeline_tasks") {
    val pipelineTaskId = long("pipeline_task_id").primaryKey().bindTo { it.pipelineTaskId }
    val pipelineId = long("pipeline_id").bindTo { it.pipelineId }
    val taskId = long("task_id").bindTo { it.taskId }
    val parentTask = long("parent_task").bindTo { it.parentTask }
    val parentTaskOrder = int("parent_task_order").bindTo { it.parentTaskOrder }

    val createStatement = """
        CREATE TABLE IF NOT EXISTS public.pipeline_tasks
        (
            pipeline_id bigint NOT NULL,
            task_id bigint NOT NULL,
            parent_task bigint NOT NULL DEFAULT 0,
            parent_task_order integer NOT NULL,
            pipeline_task_id bigint NOT NULL DEFAULT nextval('pipeline_tasks_pipeline_task_id_seq'::regclass),
            CONSTRAINT pipeline_tasks_pkey PRIMARY KEY (pipeline_task_id),
            CONSTRAINT parent_order_unique UNIQUE (pipeline_id, parent_task, parent_task_order),
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