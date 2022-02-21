package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** */
@Serializable
data class LogicField(
    /** */
    @SerialName("field_id")
    val fieldId: Long,
    /** */
    @SerialName("is_generated")
    val isGenerated: Boolean,
    /** */
    val name: String,
)
