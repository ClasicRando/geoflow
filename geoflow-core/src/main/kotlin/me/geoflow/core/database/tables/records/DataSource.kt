package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.geoflow.core.database.tables.DataSources
import me.geoflow.core.database.tables.InternalUsers
import me.geoflow.core.database.tables.Pipelines
import me.geoflow.core.database.tables.RecordWarehouseTypes

/** API response record for the [DataSources] table */
@Serializable
data class DataSource(
    /** Primary ID for a [DataSources] record */
    @SerialName("ds_id")
    val dsId: Long,
    /** Alpha numeric code for the data source */
    val code: String,
    /** Prov/state code for the record if data source is prov level */
    val prov: String?,
    /** Country of the data source */
    val country: String,
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
    val assignedUser: String,
    /** Name of the user who created the data source */
    @SerialName("created_by")
    val createdBy: String,
    /** Timestamp of the creation instant formatted */
    val created: String,
    /** Timestamp of the updated instant formatted */
    @SerialName("last_updated")
    val lastUpdated: String?,
    /** Name of the user who last update the data source */
    @SerialName("updated_by")
    val updatedBy: String?,
    /** Distance to search for adjacent record for this data source */
    @SerialName("search_radius")
    val searchRadius: Double,
    /** Warehousing type for the records in this data source */
    @SerialName("record_warehouse_type")
    val recordWarehouseType: String,
    /** Reporting type for this data source */
    @SerialName("reporting_type")
    val reportingType: String,
    /** Name of generic collection pipeline structure */
    @SerialName("collection_pipeline")
    val collectionPipeline: String,
    /** Name of generic load pipeline structure */
    @SerialName("load_pipeline")
    val loadPipeline: String?,
    /** Name of generic check pipeline structure */
    @SerialName("check_pipeline")
    val checkPipeline: String?,
    /** Name of generic qa pipeline structure */
    @SerialName("qa_pipeline")
    val qaPipeline: String?,
) {
    companion object {
        /** Generic sql to obtain a [DataSource] */
        val sql: String = """
            SELECT t1.ds_id, t1.code, t1.prov, t1.country, t1.description, t1.files_location, t1.prov_level, 
                   t1.comments, t2.name, t3.name, t1.created, t1.last_updated, t4.name, t1.search_radius, t5.name,
                   t1.reporting_type, t6.name, t7.name, t8.name, t9.name
            FROM   ${DataSources.tableName} t1
            LEFT JOIN ${InternalUsers.tableName} t2
            ON     t1.assigned_user = t2.user_oid
            LEFT JOIN ${InternalUsers.tableName} t3
            ON     t1.created_by = t3.user_oid
            LEFT JOIN ${InternalUsers.tableName} t4
            ON     t1.updated_by = t4.user_oid
            LEFT JOIN ${RecordWarehouseTypes.tableName} t5
            ON     t1.record_warehouse_type = t5.id
            LEFT JOIN ${Pipelines.tableName} t6
            ON     t1.collection_pipeline = t6.pipeline_id
            LEFT JOIN ${Pipelines.tableName} t7
            ON     t1.load_pipeline = t7.pipeline_id
            LEFT JOIN ${Pipelines.tableName} t8
            ON     t1.check_pipeline = t8.pipeline_id
            LEFT JOIN ${Pipelines.tableName} t9
            ON     t1.qa_pipeline = t9.pipeline_id
        """.trimIndent()
    }
}
