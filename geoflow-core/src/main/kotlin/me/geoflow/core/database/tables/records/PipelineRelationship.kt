package me.geoflow.core.database.tables.records

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/** */
@Serializable
data class PipelineRelationship(
    /** */
    @SerialName("st_oid")
    val stOid: Long,
    /** */
    @SerialName("parent_st_oid")
    val parentStOid: Long,
    /** */
    @SerialName("table_name")
    val tableName: String = "",
    /** */
    @SerialName("parent_table_name")
    val parentTableName: String = "",
)
