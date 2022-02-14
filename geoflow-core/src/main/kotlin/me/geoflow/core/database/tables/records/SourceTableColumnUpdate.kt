package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** */
@Serializable
data class SourceTableColumnUpdate(
    /** */
    @SerialName("stc_oid")
    val stcOid: Long,
    /** */
    val label: String,
    /** */
    @SerialName("report_group")
    val reportGroup: Int
)
