package me.geoflow.core.database.tables.records

import kotlinx.serialization.Serializable

/**
 * Data class to represent a single database record for
 * [RecordWarehouseTypes][me.geoflow.core.database.tables.RecordWarehouseTypes]
 */
@Serializable
data class RecordWarehouseType(
    /** Unique ID of the type */
    val id: Int,
    /** Common name of the type */
    val name: String,
    /** Full description of the type */
    val description: String,
)
