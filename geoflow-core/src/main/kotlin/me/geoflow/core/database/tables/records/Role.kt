package me.geoflow.core.database.tables.records

import kotlinx.serialization.Serializable

/** Data class to represent a single database record */
@Serializable
data class Role(
    /** name of role */
    val name: String,
    /** description of role */
    val description: String,
)
