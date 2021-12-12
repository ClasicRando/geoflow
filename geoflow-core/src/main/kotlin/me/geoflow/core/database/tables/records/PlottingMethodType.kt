package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Record for the [PlottingMethodTypes][me.geoflow.core.database.tables.PlottingMethodTypes] table */
@Serializable
data class PlottingMethodType(
    /** ID of the plotting method */
    @SerialName("method_id")
    val methodId: Int,
    /** Common name of the plotting method */
    val name: String,
)
