package me.geoflow.core.database.tables.records

import me.geoflow.core.database.enums.TaskStatus
import me.geoflow.core.database.tables.PipelineRunTasks
import me.geoflow.core.database.tables.QueryResultRecord
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

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
    val task: Task = Task(
        taskId = taskId,
        name = taskName,
        description = taskDescription,
        state = taskState,
        taskRunType = taskRunType,
    )

    companion object {
        /** Query to obtain an instance of the parent class */
        val sql: String = """
            SELECT t1.pr_task_id, t1.run_id, t1.task_start, t1.task_completed, t1.task_id, t2.name, t2.description,
                   t2.state, t2.task_run_type, t1.task_message, t1.parent_task_id, t1.parent_task_order,
                   t1.task_status, t1.workflow_operation, t1.task_stack_trace
            FROM   ${PipelineRunTasks.tableName} t1
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
