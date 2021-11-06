package orm.tables

import org.ktorm.schema.*
import orm.entities.Task
import orm.enums.TaskRunType
import tasks.PipelineTask
import tasks.UserTask
import tasks.SystemTask

/**
 * Table used to store generic tasks that link to a class name for execution in the worker application. Class name used
 * must be a subclass of [PipelineTask] to be runnable
 *
 * Metadata stored describes the task intent, hints as to the workflow state the task is intended to work within, and
 * defines the type of run operation the task entails (Simple [UserTask] or complex [SystemTask])
 */
object Tasks: DbTable<Task>("tasks"), SequentialPrimaryKey {
    val taskId = long("task_id").primaryKey().bindTo { it.taskId }
    val name = text("name").bindTo { it.name }
    val description = text("description").bindTo { it.description }
    val state = text("state").bindTo { it.state }
    val taskRunType = enum<TaskRunType>("task_run_type").bindTo { it.taskRunType }
    val taskClassName = text("task_class_name").bindTo { it.taskClassName }

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.tasks
        (
            name text COLLATE pg_catalog."default" NOT NULL,
            description text COLLATE pg_catalog."default" NOT NULL,
            state text COLLATE pg_catalog."default" NOT NULL,
            task_id bigint NOT NULL DEFAULT nextval('tasks_task_id_seq'::regclass),
            task_class_name text COLLATE pg_catalog."default" NOT NULL,
            task_run_type task_run_type NOT NULL,
            CONSTRAINT tasks_pkey PRIMARY KEY (task_id),
            CONSTRAINT name_unique UNIQUE (name),
            CONSTRAINT workflow_state_fk FOREIGN KEY (state)
                REFERENCES public.workflow_operations (code) MATCH SIMPLE
                ON UPDATE NO ACTION
                ON DELETE NO ACTION
                NOT VALID
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}
