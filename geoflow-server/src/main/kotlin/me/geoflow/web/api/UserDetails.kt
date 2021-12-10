package me.geoflow.web.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Details of requested user */
@Serializable
data class UserDetails(
    /** user_oid field for request */
    @SerialName("user_oid")
    val userOid: Long,
    /** name field for request */
    @SerialName("fullName")
    val name: String,
    /** username field for request */
    @SerialName("username")
    val username: String,
    /** roles field for request */
    @SerialName("roles")
    val roles: List<String>,
)
