package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** API class for simplified internal user records that have the collection role */
@Serializable
data class CollectionUser(
    /** Unique ID for internal users */
    @SerialName("user_id")
    val userid: Long,
    /** Full name of user */
    val name: String,
)
