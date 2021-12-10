package me.geoflow.core.database.tables.records

import kotlinx.serialization.Serializable

/** API response data class for JSON serialization */
@Serializable
data class WorkflowOperation(
    /** workflow operation name */
    val name: String,
    /** endpoint of workflow operation on server */
    val href: String,
)
