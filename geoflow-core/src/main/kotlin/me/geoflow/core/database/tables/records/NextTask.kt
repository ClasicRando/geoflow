package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.geoflow.core.database.enums.TaskRunType

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
