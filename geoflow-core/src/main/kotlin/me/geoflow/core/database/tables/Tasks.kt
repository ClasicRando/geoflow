package me.geoflow.core.database.tables

import me.geoflow.core.database.enums.TaskRunType
import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.tables.records.Task
import java.sql.Connection

/**
 * Table used to store generic tasks that link to a function ([SystemTask][me.geoflow.core.tasks.SystemTask]) or a const
 * value ([UserTask][me.geoflow.core.tasks.UserTask]) for execution in the worker application.
 *
 * Metadata stored describes the task intent, hints as to the workflow state the task is intended to work within, and
 * defines the type of run operation the task entails (as mentioned above).
 */
object Tasks : DbTable("tasks"), DefaultData {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.tasks
        (
			task_id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(name)) UNIQUE,
            description text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(description)),
            state text COLLATE pg_catalog."default" NOT NULL REFERENCES public.workflow_operations (code) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            task_run_type task_run_type NOT NULL
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val defaultRecordsFileName: String = "tasks.csv"

    /** Returns a list of [Task]s where the run type is User */
    fun getUserTasks(connection: Connection): List<Task> {
        return connection.submitQuery(sql = "${Task.sql} WHERE task_run_type = ?", TaskRunType.User.pgObject)
    }

    /** Returns a list of [Task]s where the run type is System */
    fun getSystemTasks(connection: Connection): List<Task> {
        return connection.submitQuery(sql = "${Task.sql} WHERE task_run_type = ?", TaskRunType.System.pgObject)
    }
}
