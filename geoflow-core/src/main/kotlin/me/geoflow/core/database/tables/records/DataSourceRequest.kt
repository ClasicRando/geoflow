package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.geoflow.core.database.tables.DataSources

/** API request object for the [DataSources] table */
@Serializable
data class DataSourceRequest(
    /** Primary ID for a [DataSources] record */
    @SerialName("ds_id")
    val dsId: Long? = null,
    /** Alpha numeric code for the data source */
    val code: String,
    /** Prov/state code for the record if data source is prov level */
    val prov: String?,
    /** Description of the data source */
    val description: String,
    /** Path to the base directory for all pipeline runs of this data source */
    @SerialName("files_location")
    val filesLocation: String,
    /** Flag denoting if the data source is for a prov/state or is federal data */
    @SerialName("prov_level")
    val provLevel: Boolean,
    /** Comments on the data source */
    val comments: String?,
    /** Name of the user assigned to the data source */
    @SerialName("assigned_user")
    val assignedUser: Long,
    /** Distance to search for adjacent record for this data source */
    @SerialName("search_radius")
    val searchRadius: Double,
    /** Warehousing type for the records in this data source */
    @SerialName("record_warehouse_type")
    val recordWarehouseType: Int,
    /** Reporting type for this data source */
    @SerialName("reporting_type")
    val reportingType: String,
    /** Name of generic collection pipeline structure */
    @SerialName("collection_pipeline")
    val collectionPipeline: Long,
    /** Name of generic load pipeline structure */
    @SerialName("load_pipeline")
    val loadPipeline: Long?,
    /** Name of generic check pipeline structure */
    @SerialName("check_pipeline")
    val checkPipeline: Long?,
    /** Name of generic qa pipeline structure */
    @SerialName("qa_pipeline")
    val qaPipeline: Long?,
)
