package database.tables

import database.enums.TaskRunType
import tasks.PipelineTask
import tasks.SystemTask
import tasks.UserTask

/**
 * Table used to store generic tasks that link to a class name for execution in the worker application. Class name used
 * must be a subclass of [PipelineTask] to be runnable
 *
 * Metadata stored describes the task intent, hints as to the workflow state the task is intended to work within, and
 * defines the type of run operation the task entails (Simple [UserTask] or complex [SystemTask])
 */
object Tasks: DbTable("tasks") {

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.tasks
        (
			task_id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(name)) UNIQUE,
            description text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(description)),
            state text COLLATE pg_catalog."default" NOT NULL REFERENCES public.workflow_operations (code) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            task_class_name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(task_class_name)),
            task_run_type task_run_type NOT NULL
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    data class Task(
        val taskId: Long,
        val name: String,
        val description: String,
        val state: String,
        val taskRunType: TaskRunType,
        val taskClassName: String,
    )
}