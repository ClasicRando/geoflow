package me.geoflow.core.database.tables.records

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** API request body for the [InternalUsers][me.geoflow.core.database.tables.InternalUsers] table */
@Serializable
data class RequestUser(
    /** user_oid field for request. Can be null for create requests */
    @SerialName("user_oid")
    val userOid: Long? = null,
    /** name field for request */
    @SerialName("fullName")
    val name: String,
    /** username field for request */
    @SerialName("username")
    val username: String,
    /** roles field for request */
    @SerialName("roles")
    val roles: List<String>,
    /** password field for request. Only non-null when create request. Never send a hashed password in response */
    @SerialName("password")
    val password: String? = null,
)
