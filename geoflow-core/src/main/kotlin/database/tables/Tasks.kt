package database.tables

import database.enums.TaskRunType
import database.extensions.queryFirstOrNull
import tasks.UserTask
import tasks.SystemTask
import java.sql.Connection
import java.sql.ResultSet

/**
 * Table used to store generic tasks that link to a function ([SystemTask]) or a const value ([UserTask]) for execution
 * in the worker application.
 *
 * Metadata stored describes the task intent, hints as to the workflow state the task is intended to work within, and
 * defines the type of run operation the task entails (as mentioned above).
 */
object Tasks : DbTable("tasks") {

    @Suppress("MaxLineLength")
    override val createStatement: String = """
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

    /** Table record for [Tasks] */
    @QueryResultRecord
    class Task (
        /** unique ID of the task */
        val taskId: Long,
        /** full name of the task */
        val name: String,
        /** description of the task functionality/intent */
        val description: String,
        /** intended workflow state that task is attached to */
        val state: String,
        taskRunType: String,
        /** class name of the task */
        val taskClassName: String,
    ) {
        /** Enum value of task run type */
        val taskRunType: TaskRunType = TaskRunType.valueOf(taskRunType)

        @Suppress("UNUSED")
        companion object {
            /** SQL query used to generate the parent class */
            val sql: String = """
                SELECT task_id, name, description, state, task_run_type, task_class_name
                FROM   $tableName
                WHERE  task_id = ?
            """.trimIndent()
            private const val TASK_ID = 1
            private const val NAME = 2
            private const val DESCRIPTION = 3
            private const val STATE = 4
            private const val TASK_RUN_TYPE = 5
            private const val TASK_CLASS_NAME = 6
            /** Function used to process a [ResultSet] into a result record */
            fun fromResultSet(rs: ResultSet): Task {
                return Task(
                    taskId = rs.getLong(TASK_ID),
                    name = rs.getString(NAME),
                    description = rs.getString(DESCRIPTION),
                    state = rs.getString(STATE),
                    taskRunType = rs.getString(TASK_RUN_TYPE),
                    taskClassName = rs.getString(TASK_CLASS_NAME),
                )
            }
        }
    }

    /** Returns a Task record for the given [taskId]. Returns null if no result is found */
    fun getRecord(connection: Connection, taskId: Long): Task? {
        return connection.queryFirstOrNull(sql = "${Task.sql} LIMIT 1", taskId)
    }
}
