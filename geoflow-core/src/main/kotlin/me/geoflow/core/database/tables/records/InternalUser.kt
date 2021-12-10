package me.geoflow.core.database.tables.records

import me.geoflow.core.database.tables.InternalUsers

/** Record for [InternalUsers]. Represents the fields of the table */
data class InternalUser(
    /** unique id of the user */
    val userOid: Long,
    /** full name of the user */
    val name: String,
    /** public username */
    val username: String,
    /** list of roles of the user. Provides access to endpoints or abilities in the api */
    val roles: List<String>,
) {
    companion object {
        /** SQL query used to generate the parent class */
        val sql: String = "SELECT user_oid, name, username, roles FROM ${InternalUsers.tableName} WHERE username = ?"
    }
}
