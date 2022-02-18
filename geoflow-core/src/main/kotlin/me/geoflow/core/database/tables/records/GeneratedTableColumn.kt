package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** */
@Serializable
data class GeneratedTableColumn(
    /** */
    @SerialName("gtc_oid")
    val gtcOid: Long? = null,
    /** */
    @SerialName("st_oid")
    val stOid: Long,
    /** */
    @SerialName("generation_expression")
    val expression: String,
    /** */
    val name: String,
    /** */
    val label: String,
    /** */
    @SerialName("report_group")
    val reportGroup: Int,
)
