package me.geoflow.core.database.tables.records

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/** */
@Serializable
data class PipelineRelationshipField(
    /** */
    @SerialName("field_id")
    val fieldId: Long,
    /** */
    @SerialName("field_is_generated")
    val fieldIsGenerated: Boolean,
    /** */
    @SerialName("parent_field_id")
    val parentFieldId: Long,
    /** */
    @SerialName("parent_field_is_generated")
    val parentFieldIsGenerated: Boolean,
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
