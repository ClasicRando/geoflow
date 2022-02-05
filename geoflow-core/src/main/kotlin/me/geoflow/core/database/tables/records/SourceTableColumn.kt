package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Record data class for [SourceTableColumns][me.geoflow.core.database.tables.SourceTableColumns] */
@Serializable
data class SourceTableColumn(
    /** unique ID of the source table column */
    @SerialName("stc_oid")
    val stcOid: Long,
    /** unique ID of the source table */
    @SerialName("st_oid")
    val stOid: Long,
    /** column name */
    val name: String,
    /** column type */
    val type: String,
    /** max length of characters of the all column values */
    @SerialName("max_length")
    val maxLength: Int,
    /** min length of characters of the all column values */
    @SerialName("min_length")
    val minLength: Int,
    /** label of column for reporting */
    val label: String,
    /** index of column within file */
    @SerialName("column_index")
    val columnIndex: Int,
    /** report grouping index, groups report sub tables */
    @SerialName("report_group")
    val reportGroup: Int
)
