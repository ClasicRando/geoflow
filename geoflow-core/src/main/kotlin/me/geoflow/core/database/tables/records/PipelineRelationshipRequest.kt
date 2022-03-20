package me.geoflow.core.database.tables.records

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/** */
@Serializable
data class PipelineRelationshipRequest(
    /** */
    @SerialName("st_oid")
    val stOid: Long,
    /** */
    @SerialName("parent_st_oid")
    val parentStOid: Long,
    /** */
    @SerialName("linking_fields")
    val linkingFields: List<PipelineRelationshipField>,
)
