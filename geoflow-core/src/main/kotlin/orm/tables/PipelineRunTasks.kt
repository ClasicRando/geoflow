package orm.tables

import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.long
import org.ktorm.schema.timestamp
import orm.entities.PipelineRunTask

object PipelineRunTasks: Table<PipelineRunTask>("pipeline_run_tasks") {

    val pipelineRunTaskId = long("pr_task_id").primaryKey().bindTo { it.pipelineRunTaskId }
    val runId = long("run_id").bindTo { it.runId }
    val taskRunning = boolean("task_running").bindTo { it.taskRunning }
    val taskComplete = boolean("task_complete").bindTo { it.taskComplete }
    val taskStart = timestamp("task_start").bindTo { it.taskStart }
    val taskCompleted = timestamp("task_completed").bindTo { it.taskCompleted }

    val createStatement = """
        CREATE TABLE IF NOT EXISTS public.pipeline_run_tasks
        (
            pr_task_id bigint NOT NULL DEFAULT nextval('pipeline_run_tasks_pr_task_id_seq'::regclass),
            run_id bigint NOT NULL,
            task_running boolean NOT NULL,
            task_complete boolean NOT NULL,
            CONSTRAINT pipeline_run_tasks_pkey PRIMARY KEY (pr_task_id)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    val createSequence = """
        CREATE SEQUENCE public.pipeline_run_tasks_pr_task_id_seq
            INCREMENT 1
            START 1
            MINVALUE 1
            MAXVALUE 9223372036854775807
            CACHE 1;
    """.trimIndent()
}