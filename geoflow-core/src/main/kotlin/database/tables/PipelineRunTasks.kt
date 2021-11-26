package database.tables

import database.NoRecordAffected
import database.NoRecordFound
import database.enums.TaskRunType
import database.enums.TaskStatus
import database.functions.GetTasksOrdered
import database.procedures.DeleteRunTaskChildren
import database.extensions.queryFirstOrNull
import database.extensions.runReturningFirstOrNull
import database.extensions.runUpdate
import database.functions.UserHasRun
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

/**
 * Table used to store tasks that are associated with a specified run
 *
 * Records contain metadata about the task, and it's run status/outcome.
 */
object PipelineRunTasks: DbTable("pipeline_run_tasks"), ApiExposed, Triggers {

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "task_status" to mapOf("name" to "Status", "formatter" to "statusFormatter"),
        "task_name" to mapOf("name" to "Task Name"),
        "task_run_type" to mapOf("name" to "Run Type"),
        "task_start" to mapOf("name" to "Start"),
        "task_completed" to mapOf("name" to "Completed"),
        "actions" to mapOf("formatter" to "taskActionFormatter"),
    )

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.pipeline_run_tasks
        (
            pr_task_id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            run_id bigint NOT NULL REFERENCES public.pipeline_runs (run_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
            task_start timestamp without time zone,
            task_completed timestamp without time zone,
            task_id bigint NOT NULL REFERENCES public.tasks (task_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            task_message text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(task_message)),
            task_status task_status NOT NULL DEFAULT 'Waiting'::task_status,
            parent_task_id bigint NOT NULL DEFAULT 0,
            parent_task_order integer NOT NULL,
            workflow_operation text COLLATE pg_catalog."default" NOT NULL
                REFERENCES public.workflow_operations (code) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            task_stack_trace text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(task_stack_trace))
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val triggers: List<Trigger> = listOf(
        Trigger(
            trigger = """
                CREATE TRIGGER record_event
                    AFTER UPDATE OR INSERT OR DELETE
                    ON public.pipeline_run_Tasks
                    FOR EACH ROW
                    EXECUTE FUNCTION public.pipeline_run_tasks_event();
            """.trimIndent(),
            triggerFunction = """
                CREATE OR REPLACE FUNCTION public.pipeline_run_tasks_event()
                    RETURNS trigger
                    LANGUAGE 'plpgsql'
                    COST 100
                    VOLATILE NOT LEAKPROOF
                AS ${'$'}BODY${'$'}
                BEGIN
                    PERFORM pg_notify(
                      'pipelineruntasks',
                      NEW.run_id::TEXT
                    );
                    RETURN NEW;
                END;
                ${'$'}BODY${'$'};
            """.trimIndent()
        )
    )

    /** Table record for [PipelineRunTasks] */
    @Suppress("LongParameterList", "UNUSED")
    @QueryResultRecord
    class PipelineRunTask private constructor(
        /** unique ID of the pipeline run task */
        val pipelineRunTaskId: Long,
        /** ID of the pipeline run this task belongs to */
        val runId: Long,
        taskStart: Timestamp?,
        taskCompleted: Timestamp?,
        taskId: Long,
        taskName: String,
        taskDescription: String,
        taskState: String,
        taskRunType: String,
        /** Message of task. Analogous to a warning message */
        val taskMessage: String?,
        /** pipeline run task ID of the parent to this task */
        val parentTaskId: Long,
        /** order within the child list of the parent task */
        val parentTaskOrder: Int,
        taskStatus: String,
        /** workflow operation that the task belongs to for the specified pipeline run */
        val workflowOperation: String,
        /** Stack trace for the exception thrown (if any) during task run */
        val taskStackTrace: String?,
    ) {
        /** [Instant] when the task run started. Converted from the provided [Timestamp] */
        val taskStart: Instant? = taskStart?.toInstant()
        /** [Instant] when the task run completed. Converted from the provided [Timestamp] */
        val taskCompleted: Instant? = taskCompleted?.toInstant()
        /** Current status of the task. Converted from the provided string into the enum value */
        val taskStatus: TaskStatus = TaskStatus.valueOf(taskStatus)
        /** Generic task the underlines the pipeline run task */
        val task: Tasks.Task = Tasks.Task(
            taskId = taskId,
            name = taskName,
            description = taskDescription,
            state = taskState,
            taskRunType = taskRunType,
        )

        @Suppress("UNUSED")
        companion object {
            /**  */
            val sql: String = """
                SELECT t1.pr_task_id, t1.run_id, t1.task_start, t1.task_completed, t1.task_id, t2.name, t2.description,
                       t2.state, t2.task_run_type, t1.task_message, t1.parent_task_id, t1.parent_task_order,
                       t1.task_status, t1.workflow_operation, t1.task_stack_trace
                FROM   $tableName t1
                JOIN   tasks t2
                ON     t1.task_id = t2.task_id
                WHERE  pr_task_id = ?
            """.trimIndent()
            private const val PR_TASK_ID = 1
            private const val RUN_ID = 2
            private const val TASK_START = 3
            private const val TASK_COMPLETED = 4
            private const val TASK_ID = 5
            private const val TASK_NAME = 6
            private const val TASK_DESCRIPTION = 7
            private const val TASK_STATE = 8
            private const val TASK_RUN_TYPE = 9
            private const val TASK_MESSAGE = 10
            private const val PARENT_TASK_ID = 11
            private const val PARENT_TASK_ORDER = 12
            private const val TASK_STATUS = 13
            private const val WORKFLOW_OPERATION = 14
            private const val TASK_STACK_TRACE = 15

            /** Function used to process a [ResultSet] into a result record */
            fun fromResultSet(rs: ResultSet): PipelineRunTask {
                require(!rs.isBeforeFirst) { "ResultSet must be at or after first record" }
                require(!rs.isClosed) { "ResultSet is closed" }
                require(!rs.isAfterLast) { "ResultSet has no more rows to return" }
                return PipelineRunTask(
                    pipelineRunTaskId = rs.getLong(PR_TASK_ID),
                    runId = rs.getLong(RUN_ID),
                    taskStart = rs.getTimestamp(TASK_START),
                    taskCompleted = rs.getTimestamp(TASK_COMPLETED),
                    taskId = rs.getLong(TASK_ID),
                    taskName = rs.getString(TASK_NAME),
                    taskDescription = rs.getString(TASK_DESCRIPTION),
                    taskState = rs.getString(TASK_STATE),
                    taskRunType = rs.getString(TASK_RUN_TYPE),
                    taskMessage = rs.getString(TASK_MESSAGE),
                    parentTaskId = rs.getLong(PARENT_TASK_ID),
                    parentTaskOrder = rs.getInt(PARENT_TASK_ORDER),
                    taskStatus = rs.getString(TASK_STATUS),
                    workflowOperation = rs.getString(WORKFLOW_OPERATION),
                    taskStackTrace = rs.getString(TASK_STACK_TRACE),
                )
            }
        }
    }

    /**
     * Locks to the current connection and returns the [PipelineRunTask] specified by the [pipelineRunTaskId]
     *
     * @throws IllegalArgumentException when the provided ID returns no records
     */
    fun getWithLock(connection: Connection, pipelineRunTaskId: Long): PipelineRunTask {
        return connection.queryFirstOrNull(
            sql = "${PipelineRunTask.sql} FOR UPDATE",
            pipelineRunTaskId
        ) ?: throw IllegalArgumentException("ID provided did not match a record in the database")
    }

    /** Returns the [PipelineRunTask] specified by the provided [pipelineRunTaskId] or null if no record can be found */
    @Suppress("UNUSED")
    fun getRecord(connection: Connection, pipelineRunTaskId: Long): PipelineRunTask? {
        return connection.queryFirstOrNull(
            sql = PipelineRunTask.sql,
            pipelineRunTaskId,
        )
    }

    /**
     * Returns [NextTask] instance representing the next runnable task for the given [runId]. Verifies that the
     * [userOid] has the ability to run tasks for this [runId].
     *
     * @throws IllegalArgumentException when the user is not able to run tasks for the run or the next task to run
     * cannot be found
     */
    fun getRecordForRun(connection: Connection, userOid: Long, runId: Long): NextTask {
        require(UserHasRun.checkUserRun(connection, userOid, runId)) {
            "User provided cannot run tasks for this pipeline run"
        }
        return getNextTask(connection, runId) ?: throw NoRecordFound(tableName, message = "Cannot find next task")
    }

    /**
     * Reset task to waiting state and deletes all children tasks using a stored procedure
     *
     * @throws NoRecordAffected various cases can throw this exception. These include:
     * - the user is not able to run tasks for the run
     * - there is no record with the [pipelineRunTaskId] specified
     * - the record obtained is waiting to be scheduled
     */
    fun resetRecord(connection: Connection, userOid: Long, pipelineRunTaskId: Long) {
        val sql = """
            UPDATE $tableName
            SET    task_status = ?,
                   task_completed = null,
                   task_start = null,
                   task_message = null,
                   task_stack_trace = null
            WHERE  pr_task_id = ?
            AND    task_status != ?
            AND    user_has_run(?, run_id)
        """.trimIndent()
        val updateCount = connection.runUpdate(
            sql = sql,
            TaskStatus.Waiting.pgObject,
            pipelineRunTaskId,
            TaskStatus.Waiting.pgObject,
            userOid,
        )
        if (updateCount == 1) {
            DeleteRunTaskChildren.call(connection, pipelineRunTaskId)
        } else {
            throw NoRecordAffected(
                tableName,
                "No records were reset. Make sure the provided run_id matches the task and the task is Waiting"
            )
        }
    }

    /**
     * Adds the desired generic task as the last child of the [pipelineRunTaskId]
     */
    fun addTask(connection: Connection, pipelineRunTaskId: Long, taskId: Long): Long? {
        val sql = """
            INSERT INTO $tableName (run_id,task_status,task_id,parent_task_id,parent_task_order,workflow_operation) 
            SELECT distinct t1.run_id, ?, ?, t1.pr_task_id,
                   COALESCE(
                      MAX(t2.parent_task_order) OVER (PARTITION BY t2.parent_task_id ) + 1,
                      1
                    ),
                   t1.workflow_operation
            FROM   $tableName t1
            LEFT JOIN $tableName t2 on t1.pr_task_id = t2.parent_task_id
            WHERE  t1.pr_task_id = ?
            RETURNING pr_task_id
        """.trimIndent()
        return connection.runReturningFirstOrNull(
            sql = sql,
            TaskStatus.Waiting.pgObject,
            taskId,
            pipelineRunTaskId,
        )
    }

    /** API response data class for JSON serialization */
    @Serializable
    data class Record(
        /** Order of the task within the current view of all tasks for a provided pipeline run */
        @SerialName("task_order")
        val taskOrder: Long,
        /** unique ID of the pipeline run task */
        @SerialName("pipeline_run_task_id")
        val pipelineRunTaskId: Long,
        /** ID of the pipeline run this task belongs to */
        @SerialName("run_id")
        val runId: Long,
        /** Formatted timestamp of the starting instant of the task run */
        @SerialName("task_start")
        val taskStart: String?,
        /** Formatted timestamp of the completion instant of the task run */
        @SerialName("task_completed")
        val taskCompleted: String?,
        /** unique ID of the generic underlining task run */
        @SerialName("task_id")
        val taskId: Long,
        /** Message of task. Analogous to a warning message */
        @SerialName("task_message")
        val taskMessage: String?,
        /** Current status of the task. Name of the enum value */
        @SerialName("task_status")
        val taskStatus: String,
        /** pipeline run task ID of the parent to this task */
        @SerialName("parent_task_id")
        val parentTaskId: Long,
        /** order within the child list of the parent task */
        @SerialName("parent_task_order")
        val parentTaskOrder: Int,
        /** workflow operation that the task belongs to for the specified pipeline run */
        @SerialName("workflow_operation")
        val workflowOperation: String,
        /** Stack trace for the exception thrown (if any) during task run */
        @SerialName("task_stack_trace")
        val taskStackTrace: String?,
        /** Name of the underlining task run */
        @SerialName("task_name")
        val taskName: String,
        /** Description of the underlining task */
        @SerialName("task_description")
        val taskDescription: String,
        /** Run type of the underling task. Name of the enum value */
        @SerialName("task_run_type")
        val taskRunType: String,
    )

    /**
     * Returns all the tasks associated with the provided [runId], ordered by the parent child relationships and
     * relative ordering within those relationships
     */
    fun getOrderedTasks(connection: Connection, runId: Long): List<Record> {
        return GetTasksOrdered.getTasks(connection, runId)
    }

    @Serializable
    /** Holds minimal details of the next available task to run */
    data class NextTask(
        @SerialName("pipeline_run_task_id")
        /** unique ID of the pipeline run task */
        val pipelineRunTaskId: Long,
        @SerialName("task_id")
        /** unique ID of the generic underlining task run */
        val taskId: Long,
        @SerialName("task_run_type")
        /** Run type of the underling task as the enum value */
        val taskRunType: TaskRunType,
    )

    /**
     * Returns the next runnable task for the given [runId] as a [NextTask]
     *
     * @throws IllegalArgumentException when a task in the run is currently running or scheduled
     */
    fun getNextTask(connection: Connection, runId: Long): NextTask? {
        val running = connection.queryFirstOrNull<Long?>(
            sql = "SELECT task_id FROM $tableName WHERE run_id = ? AND task_status in (?,?) LIMIT 1",
            runId,
            TaskStatus.Scheduled.pgObject,
            TaskStatus.Running.pgObject,
        )
        require(running == null) { "Task currently scheduled/running (id = $running)" }
        return getOrderedTasks(connection, runId).firstOrNull {
            it.taskStatus == TaskStatus.Waiting.name
        }?.let { record ->
            NextTask(
                record.pipelineRunTaskId,
                record.taskId,
                TaskRunType.valueOf(record.taskRunType),
            )
        }
    }

    /**
     * Updates the status for the given record
     */
    fun setStatus(connection: Connection, pipelineRunTaskId: Long, status: TaskStatus) {
        connection.runUpdate(
            sql = "UPDATE $tableName SET task_status = ? WHERE pr_Task_id = ?",
            status.pgObject,
            pipelineRunTaskId,
        )
    }

    private object NonUpdatedField

    /**
     * Generic update where some fields can be excluded from the function call and the update will not include them.
     * Uses empty singleton to do referential checking to see which fields were provided for update.
     */
    @Suppress("LongParameterList", "ComplexMethod")
    fun update(
        connection: Connection,
        pipelineRunTaskId: Long,
        taskStatus: Any = NonUpdatedField,
        taskStart: Any? = NonUpdatedField,
        taskCompleted: Any? = NonUpdatedField,
        taskMessage: Any? = NonUpdatedField,
        taskStackTrace: Any? = NonUpdatedField,
    ) {
        val updates = mutableMapOf<String, Any?>()
        if (taskStatus !== NonUpdatedField) {
            updates["task_status"] = if (taskStatus is TaskStatus) {
                taskStatus.pgObject
            } else {
                error("taskStatus must be a TaskStatus")
            }
        }
        if (taskStart !== NonUpdatedField) {
            updates["task_start"] = when (taskStart) {
                is Timestamp? -> taskStart
                is Instant? -> taskStart?.let { Timestamp(it.toEpochMilli()) }
                else -> error("taskStart must be an Instant or Timestamp")
            }
        }
        if (taskCompleted !== NonUpdatedField) {
            updates["task_completed"] = when (taskCompleted) {
                is Timestamp? -> taskCompleted
                is Instant? -> taskCompleted?.let { Timestamp(it.toEpochMilli()) }
                else -> error("taskCompleted must be an Instant or Timestamp")
            }
        }
        if (taskMessage !== NonUpdatedField) {
            updates["task_message"] = if (taskMessage is String?) {
                taskMessage
            } else {
                error("taskMessage must be a String")
            }
        }
        if (taskStackTrace !== NonUpdatedField) {
            updates["task_stack_trace"] = if (taskStackTrace is String?) {
                taskStackTrace
            } else {
                error("taskStackTrace must be a String")
            }
        }
        val sortedMap = updates.toSortedMap()
        val sql = """
            UPDATE $tableName
            SET    ${sortedMap.entries.joinToString { "${it.key} = ?" }}
            WHERE  pr_task_id = ?
        """.trimIndent()
        connection.runUpdate(
            sql = sql,
            sortedMap.values,
            pipelineRunTaskId,
        )
    }
}
