package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** API response data class for JSON serialization */
@Serializable
data class ResponsePrTask(
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
    /** HTML used in the task output modal */
    @SerialName("modal_html")
    val modalHtml: String?,
)
