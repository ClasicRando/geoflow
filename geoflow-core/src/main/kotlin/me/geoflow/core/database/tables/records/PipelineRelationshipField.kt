package me.geoflow.core.database.tables.records

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/** */
@Serializable
data class PipelineRelationshipField(
    /** */
    @SerialName("stc_oid")
    val stcOid: Long,
    /** */
    @SerialName("parent_stc_oid")
    val parentStcOid: Long,
    /** */
    @SerialName("st_oid")
    val stOid: Long,
    /** */
    @SerialName("column_name")
    val columnName: String = "",
    /** */
    @SerialName("parent_column_name")
    val parentColumnName: String = "",
)
