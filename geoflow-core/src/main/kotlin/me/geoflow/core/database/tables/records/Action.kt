package me.geoflow.core.database.tables.records

import kotlinx.serialization.Serializable

/** API response data class for JSON serialization */
@Serializable
data class Action(
    /** action name */
    val name: String,
    /** action description */
    val description: String,
    /** endpoint that allows for an action to be performed */
    val href: String,
)
