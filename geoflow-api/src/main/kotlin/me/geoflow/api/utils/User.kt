package me.geoflow.api.utils

import kotlinx.serialization.Serializable

/** */
@Serializable
data class User(
    /** */
    val username: String,
    /** */
    val password: String,
)