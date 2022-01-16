package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Record for the [PlottingMethods][me.geoflow.core.database.tables.PlottingMethods] table */
@Serializable
data class PlottingMethod(
    /** Pipeline run ID that is linked to this record */
    @SerialName("run_id")
    val runId: Long,
    /** Order at which the plotting method is used within this run */
    val order: Int,
    /**
     * Plotting method used for geocoding. Links to an ID in the
     * [PlottingMethodTypes][me.geoflow.core.database.tables.PlottingMethodTypes] table
     */
    @SerialName("method_type")
    val methodType: Int,
    /** Plotting method name from [PlottingMethodTypes][me.geoflow.core.database.tables.PlottingMethodTypes] table */
    @SerialName("method_name")
    val methodName: String,
    /**
     * Unique ID in reference to [SourceTables][me.geoflow.core.database.tables.SourceTables] for the specified [runId]
     */
    @SerialName("st_oid")
    val stOid: Long,
    /**
     * Name of table referenced in [SourceTables][me.geoflow.core.database.tables.SourceTables]
     */
    @SerialName("table_name")
    val tableName: String,
)
