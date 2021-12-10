package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.geoflow.core.database.enums.FileCollectType
import me.geoflow.core.database.enums.LoaderType

/** API response data class for JSON serialization */
@Serializable
data class SourceTable(
    /** unique ID of the pipeline run */
    @SerialName("st_oid")
    val stOid: Long,
    /** database table name the file (and sub table if needed) */
    @SerialName("table_name")
    val tableName: String,
    /** filed id of the source table. Used as a runId specific code */
    @SerialName("file_id")
    val fileId: String,
    /** source filename for the source table */
    @SerialName("file_name")
    val fileName: String,
    /** if the file has sub tables (ie mdb or excel), the name if used to collect the right data */
    @SerialName("sub_table")
    val subTable: String?,
    /** delimiter of the data, if required */
    @SerialName("delimiter")
    val delimiter: String?,
    /** flag denoting if the data is qualified, if required */
    @SerialName("qualified")
    val qualified: Boolean,
    /** encoding of the file */
    @SerialName("encoding")
    val encoding: String,
    /** url to obtain the data */
    @SerialName("url")
    val url: String?,
    /** comments about the source table */
    @SerialName("comments")
    val comments: String?,
    /** scanned record count of the source table */
    @SerialName("record_count")
    val recordCount: Int,
    /** collection method to obtain the file. Name of the enum value */
    @SerialName("collect_type")
    val collectType: String,
    /** flag denoting if the source table has been analyzed */
    @SerialName("analyze")
    val analyze: Boolean,
    /** flag denoting if the source table has been loaded */
    @SerialName("load")
    val load: Boolean,
) {
    /** classification of the loader type for the file. Name of the enum value */
    val loaderType: LoaderType get() = LoaderType.getLoaderType(fileName)
    /** */
    val fileCollectType: FileCollectType get() = FileCollectType.valueOf(collectType)

    /** Initialization function to validate serialization results */
    init {
        require(runCatching { LoaderType.getLoaderType(fileName) }.isSuccess) {
            "string value passed for LoaderType is not valid"
        }
        require(runCatching { FileCollectType.valueOf(collectType) }.isSuccess) {
            "string value passed for FileCollectType is not valid"
        }
        val validSubTable = if (loaderType in setOf(LoaderType.Excel, LoaderType.MDB)) {
            subTable != null && subTable.isNotBlank()
        } else {
            subTable.isNullOrBlank()
        }
        require(validSubTable) { "sub table must be a valid input" }
    }
}
