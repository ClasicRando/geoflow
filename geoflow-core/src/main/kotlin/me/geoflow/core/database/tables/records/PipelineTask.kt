package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class to represent a single database record for [PipelineTasks][me.geoflow.core.database.tables.PipelineTasks]
 */
@Serializable
data class PipelineTask(
    /** Unique id for a generic pipeline task */
    @SerialName("pipeline_task_id")
    val pipelineTaskId: Long? = null,
    /** ID of the parent pipeline */
    @SerialName("pipeline_id")
    val pipelineId: Long,
    /** ID of the task called for pipeline step */
    @SerialName("task_id")
    val taskId: Long,
    /** Order within the parent pipeline */
    @SerialName("task_order")
    val taskOrder: Int,
)
