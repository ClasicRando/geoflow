package me.geoflow.web.auth

import kotlinx.serialization.Serializable

/** Structure of a POST call to the login endpoint. Provides details similar to a form sign-in request  */
@Serializable
data class User(
    /** username provided in the login form */
    val username: String,
    /** password provided in the login form */
    val password: String,
)
