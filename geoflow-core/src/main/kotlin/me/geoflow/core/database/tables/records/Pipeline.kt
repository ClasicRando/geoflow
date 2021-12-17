package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class to represent a single database record for [Pipelines][me.geoflow.core.database.tables.Pipelines]
 */
@Serializable
data class Pipeline(
    /** Unique ID of a generic pipeline */
    @SerialName("pipeline_id")
    val pipelineId: Long? = null,
    /** Common name of the pipeline */
    val name: String,
    /** Linked workflow operation of the pipeline */
    @SerialName("workflow_operation")
    val workflowOperation: String,
)
