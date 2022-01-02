package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** */
@Serializable
data class TableCountComparison(
    /** */
    @SerialName("st_oid")
    val stOid: Long,
    /** */
    @SerialName("current_table_name")
    val currentTableName: String,
    /** */
    @SerialName("current_file_name")
    val currentFileName: String,
    /** */
    @SerialName("current_record_count")
    val currentRecordCount: Int,
    /** */
    @SerialName("last_table_name")
    val lastTableName: String,
    /** */
    @SerialName("last_file_name")
    val lastFileName: String,
    /** */
    @SerialName("last_record_count")
    val lastRecordCount: Int,
)
