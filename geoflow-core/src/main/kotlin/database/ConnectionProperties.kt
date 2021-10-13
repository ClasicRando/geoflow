package database

import kotlinx.serialization.Serializable

@Serializable
data class ConnectionProperties(
    val className: String,
    val url: String,
    val username: String,
    val password: String,
    val serverName: String,
    val dbName: String,
)
