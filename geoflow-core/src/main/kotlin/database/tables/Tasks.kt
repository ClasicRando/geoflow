package database.tables

import database.enums.TaskRunType
import database.queryFirstOrNull
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

    class Task(
        val taskId: Long,
        val name: String,
        val description: String,
        val state: String,
        taskRunType: String,
        val taskClassName: String,
    ) {
        val taskRunType = TaskRunType.valueOf(taskRunType)

        companion object {
            fun fromResultSet(rs: ResultSet): Task {
                require(!rs.isBeforeFirst) { "ResultSet must be at or after first record" }
                require(!rs.isClosed) { "ResultSet is closed" }
                require(!rs.isAfterLast) { "ResultSet has no more rows to return" }
                return Task(
                    taskId = rs.getLong(1),
                    name = rs.getString(2),
                    description = rs.getString(3),
                    state = rs.getString(4),
                    taskRunType = rs.getString(5),
                    taskClassName = rs.getString(6),
                )
            }
        }
    }

    fun getRecord(connection: Connection, taskId: Long): Task? {
        val sql = """
            SELECT task_id, name, description, state, task_run_type, task_class_name
            FROM   $tableName
            WHERE  task_id = ?
            LIMIT 1
        """.trimIndent()
        return connection.queryFirstOrNull(sql = sql, taskId)
    }
}