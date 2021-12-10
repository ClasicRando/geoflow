package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** API response data class for JSON serialization */
@Serializable
data class ResponseUser(
    /** unique id of the user */
    @SerialName("user_oid")
    val userOid: Long,
    /** full name of the user */
    val name: String,
    /** public username */
    val username: String,
    /** list of roles of the user */
    val roles: String,
    /** flag denoting if the user can be edited by the requesting user */
    @SerialName("can_edit")
    val canEdit: Boolean,
)
