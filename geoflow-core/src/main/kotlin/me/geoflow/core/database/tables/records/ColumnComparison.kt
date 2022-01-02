package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** */
@Serializable
data class ColumnComparison(
    /** */
    @SerialName("current_name")
    val currentName: String,
    /** */
    @SerialName("current_type")
    val currentType: String,
    /** */
    @SerialName("current_max_length")
    val currentMaxLength: Int,
    /** */
    @SerialName("last_name")
    val lastName: Int,
    /** */
    @SerialName("last_type")
    val lastType: String,
    /** */
    @SerialName("last_max_length")
    val lastMaxLength: Int,
)
