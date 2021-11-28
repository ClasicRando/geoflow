package database.tables

import database.enums.TaskRunType
import database.extensions.queryFirstOrNull
import database.extensions.submitQuery
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.Connection

/**
 * Table used to store generic tasks that link to a function ([SystemTask][tasks.SystemTask]) or a const value
 * ([UserTask][tasks.UserTask]) for execution in the worker application.
 *
 * Metadata stored describes the task intent, hints as to the workflow state the task is intended to work within, and
 * defines the type of run operation the task entails (as mentioned above).
 */
object Tasks : DbTable("tasks"), DefaultData {

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
            task_run_type task_run_type NOT NULL
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val defaultRecordsFileName: String = "tasks.csv"

    /** Table record for [Tasks] */
    @Suppress("unused")
    @Serializable
    data class Task (
        /** unique ID of the task */
        @SerialName("task_id")
        val taskId: Long,
        /** full name of the task */
        val name: String,
        /** description of the task functionality/intent */
        val description: String,
        /** intended workflow state that task is attached to */
        val state: String,
        /** string value of task run type */
        @SerialName("task_run_type")
        val taskRunType: String,
    ) {
        /** Enum value of task run type */
        val taskRunTypeEnum: TaskRunType get() = TaskRunType.valueOf(taskRunType)

        init {
            require(runCatching { TaskRunType.valueOf(taskRunType) }.isSuccess) {
                "string value passed for TaskRunType is not valid"
            }
        }

        companion object {
            /** SQL query used to generate the parent class */
            val sql: String = "SELECT task_id, name, description, state, task_run_type FROM $tableName"
        }
    }

    /** Returns a Task record for the given [taskId]. Returns null if no result is found */
    fun getRecord(connection: Connection, taskId: Long): Task? {
        return connection.queryFirstOrNull(sql = "${Task.sql} WHERE task_id = ?", taskId)
    }

    /** Returns a list of [Task]s where the run type is User */
    fun getUserTasks(connection: Connection): List<Task> {
        return connection.submitQuery(sql = "${Task.sql} WHERE task_run_type = ?", TaskRunType.User.pgObject)
    }
}
