package database.tables

import database.enums.TaskRunType
import database.enums.TaskStatus
import database.functions.GetTasksOrdered
import database.procedures.DeleteRunTaskChildren
import database.queryFirstOrNull
import database.runReturningFirstOrNull
import database.runUpdate
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
object PipelineRunTasks: DbTable("pipeline_run_tasks"), ApiExposed {

    override val tableDisplayFields = mapOf(
        "task_status" to mapOf("name" to "Status", "formatter" to "statusFormatter"),
        "task_name" to mapOf("name" to "Task Name"),
        "task_run_type" to mapOf("name" to "Run Type"),
        "task_start" to mapOf("name" to "Start"),
        "task_completed" to mapOf("name" to "Completed"),
        "actions" to mapOf("formatter" to "taskActionFormatter"),
    )

    override val createStatement = """
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
            workflow_operation text COLLATE pg_catalog."default" NOT NULL REFERENCES public.workflow_operations (code) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            task_stack_trace text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(task_stack_trace))
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    private val genericSql = """
        SELECT t1.pr_task_id, t1.run_id, t1.task_start, t1.task_completed, t1.task_id, t2.name, t2.description,
               t2.state, t2.task_run_type, t2.task_class_name, t1.task_message, t1.parent_task_id,
               t1.parent_task_order, t1.task_status, t1.workflow_operation, t1.task_stack_trace
        FROM   $tableName t1
        JOIN   tasks t2
        ON     t1.task_id = t2.task_id
        WHERE  pr_task_id = ?
    """

    class PipelineRunTask private constructor(
        val pipelineRunTaskId: Long,
        val runId: Long,
        taskStart: Timestamp?,
        taskCompleted: Timestamp?,
        taskId: Long,
        taskName: String,
        taskDescription: String,
        taskState: String,
        taskRunType: String,
        taskClassName: String,
        val taskMessage: String?,
        val parentTaskId: Long,
        val parentTaskOrder: Int,
        taskStatus: String,
        val workflowOperation: String,
        val taskStackTrace: String?,
    ) {
        val taskStart = taskStart?.toInstant()
        val taskCompleted = taskCompleted?.toInstant()
        val taskStatus = TaskStatus.valueOf(taskStatus)
        val task = Tasks.Task(
            taskId = taskId,
            name = taskName,
            description = taskDescription,
            state = taskState,
            taskRunType = TaskRunType.valueOf(taskRunType),
            taskClassName = taskClassName,
        )

        companion object {
            fun fromResultSet(rs: ResultSet): PipelineRunTask {
                require(!rs.isBeforeFirst) { "ResultSet must be at or after first record" }
                require(!rs.isClosed) { "ResultSet is closed" }
                require(!rs.isAfterLast) { "ResultSet has no more rows to return" }
                return PipelineRunTask(
                    pipelineRunTaskId = rs.getLong(1),
                    runId = rs.getLong(2),
                    taskStart = rs.getTimestamp(3),
                    taskCompleted = rs.getTimestamp(4),
                    taskId = rs.getLong(5),
                    taskName = rs.getString(6),
                    taskDescription = rs.getString(7),
                    taskState = rs.getString(8),
                    taskRunType = rs.getString(9),
                    taskClassName = rs.getString(10),
                    taskMessage = rs.getString(11),
                    parentTaskId = rs.getLong(12),
                    parentTaskOrder = rs.getInt(13),
                    taskStatus = rs.getString(14),
                    workflowOperation = rs.getString(15),
                    taskStackTrace = rs.getString(16),
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
            sql = "$genericSql FOR UPDATE",
            pipelineRunTaskId
        ) ?: throw IllegalArgumentException("ID provided did not match a record in the database")
    }

    /** Returns the [PipelineRunTask] specified by the provided [pipelineRunTaskId] or null if no record can be found */
    fun getRecord(connection: Connection, pipelineRunTaskId: Long): PipelineRunTask? {
        return connection.queryFirstOrNull(
            sql = genericSql,
            pipelineRunTaskId,
        )
    }

    /**
     * Returns [NextTask] instance representing the next runnable task for the given [runId]. Verifies that the
     * [username] has the ability to run tasks for this [runId].
     *
     * @throws IllegalArgumentException when the user is not able to run tasks for the run or the next task to run
     * cannot be found
     */
    fun getRecordForRun(connection: Connection, username: String, runId: Long): NextTask {
        require(PipelineRuns.checkUserRun(connection, runId, username)) {
            "User provided cannot run tasks for this pipeline run"
        }
        return getNextTask(connection, runId) ?: throw IllegalArgumentException("Cannot find next task")
    }

    /**
     * Reset task to waiting state and deletes all children tasks using a stored procedure
     *
     * @throws IllegalArgumentException various cases can throw this exception (with a specific message). These include
     * - the user is not able to run tasks for the run
     * - the record count not be found
     * - the record obtained has a different runId
     * - the record obtained is waiting to be scheduled
     */
    fun resetRecord(connection: Connection, username: String, runId: Long, pipelineRunTaskId: Long) {
        require(PipelineRuns.checkUserRun(connection, runId, username)) {
            "User provided cannot run tasks for this pipeline run"
        }
        val sql = """
            UPDATE $tableName
            SET    task_status = ?,
                   task_completed = null,
                   task_start = null,
                   task_message = null,
                   task_stack_trace = null
            WHERE  pr_task_id = ?
            AND    task_status != ?
            AND    run_id = ?
        """.trimIndent()
        val updateCount = connection.runUpdate(
            sql = sql,
            TaskStatus.Waiting.pgObject,
            pipelineRunTaskId,
            TaskStatus.Waiting.pgObject,
            runId
        )
        if (updateCount == 1) {
            DeleteRunTaskChildren.call(connection, pipelineRunTaskId)
        } else {
            throw IllegalArgumentException(
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
        @SerialName("task_order")
        val taskOrder: Long,
        @SerialName("pipeline_run_task_id")
        val pipelineRunTaskId: Long,
        @SerialName("run_id")
        val runId: Long,
        @SerialName("task_start")
        val taskStart: String?,
        @SerialName("task_completed")
        val taskCompleted: String?,
        @SerialName("task_id")
        val taskId: Long,
        @SerialName("task_message")
        val taskMessage: String?,
        @SerialName("task_status")
        val taskStatus: String,
        @SerialName("parent_task_id")
        val parentTaskId: Long,
        @SerialName("parent_task_order")
        val parentTaskOrder: Int,
        @SerialName("workflow_operation")
        val workflowOperation: String,
        @SerialName("task_stack_trace")
        val taskStackTrace: String?,
        @SerialName("task_name")
        val taskName: String,
        @SerialName("task_description")
        val taskDescription: String,
        @SerialName("task_class_name")
        val taskClassName: String,
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

    /** Holds minimal details of the next available task to run */
    data class NextTask(
        val pipelineRunTaskId: Long,
        val taskId: Long,
        val taskRunType: TaskRunType,
        val taskClassName: String,
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
        return getOrderedTasks(connection, runId).firstOrNull{
            it.taskStatus == TaskStatus.Waiting.name
        }?.let { record ->
            NextTask(
                record.pipelineRunTaskId,
                record.taskId,
                TaskRunType.valueOf(record.taskRunType),
                record.taskClassName
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
            updates["task_status"] = if(taskStatus is TaskStatus) {
                taskStatus.pgObject
            } else {
                throw IllegalArgumentException("taskStatus must be a TaskStatus")
            }
        }
        if (taskStart !== NonUpdatedField) {
            updates["task_start"] = when(taskStart) {
                is Timestamp? -> taskStart
                is Instant? -> taskStart?.let { Timestamp(it.toEpochMilli()) }
                else -> throw IllegalArgumentException("taskStart must be an Instant or Timestamp")
            }
        }
        if (taskCompleted !== NonUpdatedField) {
            updates["task_completed"] = when(taskCompleted) {
                is Timestamp? -> taskCompleted
                is Instant? -> taskCompleted?.let { Timestamp(it.toEpochMilli()) }
                else -> throw IllegalArgumentException("taskCompleted must be an Instant or Timestamp")
            }
        }
        if (taskMessage !== NonUpdatedField) {
            updates["task_message"] = if(taskMessage is String?) {
                taskMessage
            } else {
                throw IllegalArgumentException("taskMessage must be a String")
            }
        }
        if (taskStackTrace !== NonUpdatedField) {
            updates["task_stack_trace"] = if(taskStackTrace is String?) {
                taskStackTrace
            } else {
                throw IllegalArgumentException("taskStackTrace must be a String")
            }
        }
        val sortedMap = updates.toSortedMap()
        val sql = """
            UPDATE $tableName
            SET    ${sortedMap.entries.joinToString { "${it.key} = ?" }}
            WHERE  pr_task_id = ?
        """.trimIndent()
        connection.runUpdate(sql = sql, *sortedMap.values.toTypedArray(), pipelineRunTaskId)
    }
}
