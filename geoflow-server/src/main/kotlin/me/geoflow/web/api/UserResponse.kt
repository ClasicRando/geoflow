package me.geoflow.web.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** API response for a single set of user details */
@Serializable
data class UserResponse(
    /** Payload of details from response */
    val payload: UserDetails,
    /** object type of response */
    @SerialName("object")
    val objectType: String,
)
