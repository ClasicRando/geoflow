package me.geoflow.core.database.tables

import me.geoflow.core.database.errors.NoRecordAffected
import me.geoflow.core.database.extensions.queryFirstOrNull
import me.geoflow.core.database.extensions.queryHasResult
import me.geoflow.core.database.extensions.runReturningFirstOrNull
import me.geoflow.core.database.extensions.submitQuery
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.geoflow.core.database.errors.NoRecordFound
import java.sql.Connection

/**
 * Table used to store users of the web server interface of the application
 *
 * General user definition with username, password (hashed) and roles provided to the user. This table is only used
 * during the user validation/login phase. After that point, the session contains the user information needed
 */
object InternalUsers : DbTable("internal_users"), ApiExposed {

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "user_oid" to mapOf("name" to "User ID"),
        "name" to mapOf("name" to "Full Name"),
        "username" to mapOf(),
        "is_admin" to mapOf("name" to "Admin?", "formatter" to "isAdminFormatter"),
        "roles" to mapOf(),
        "can_edit" to mapOf("name" to "Edit", "formatter" to "editFormatter"),
    )

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.internal_users
        (
            user_oid bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(name)),
            username text COLLATE pg_catalog."default" UNIQUE NOT NULL CHECK (check_not_blank_or_empty(username)),
            password text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(password)),
            roles text[] COLLATE pg_catalog."default" NOT NULL CHECK (check_array_not_blank_or_empty(roles))
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    /** Validation response as a sealed interface of success or failure (standard message) */
    sealed interface ValidationResponse {
        /** Object to denote a successful validation of user */
        object Success : ValidationResponse
        /** Object to denote a failed validation of user */
        object Failure : ValidationResponse {
            /** Stock error message on validation failure */
            const val ERROR_MESSAGE: String = "Incorrect username or password"
        }
    }

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
            val sql: String = "SELECT user_oid, name, username, roles FROM $tableName WHERE username = ?"
        }
    }

    /**
     * Tries to look up the username provided and validate the password if the user can be found.
     *
     * If the user cannot be found or the password is incorrect, the [Failure][ValidationResponse.Failure] object is
     * returned. Otherwise, [Success][ValidationResponse.Success] is returned
     *
     * @throws [java.sql.SQLException] when the connection throws an error
     */
    fun validateUser(connection: Connection, username: String, password: String): ValidationResponse {
        val sql = """
            SELECT (password = crypt(?,password))
            FROM   $tableName
            WHERE  username = ?
        """.trimIndent()
        return if (connection.queryFirstOrNull<Boolean>(sql, password, username) == true) {
            ValidationResponse.Success
        } else {
            ValidationResponse.Failure
        }
    }

    /**
     * Returns the [InternalUser] entity if the username can be found.
     *
     * @throws NoRecordFound when the query returns an empty result
     * @throws [java.sql.SQLException] when the connection throws an error
     */
    fun getUser(connection: Connection, username: String): InternalUser {
        return connection.queryFirstOrNull(
            sql = InternalUser.sql,
            username
        ) ?: throw NoRecordFound(tableName, "User cannot be found")
    }

    /** API request to [InternalUsers] table */
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

    /**
     * Returns the [InternalUser] entity if the username can be found.
     *
     * @throws NoRecordFound when the query returns an empty result
     * @throws [java.sql.SQLException] when the connection throws an error
     */
    fun getSelf(connection: Connection, userOid: Long): RequestUser {
        return connection.queryFirstOrNull(
            sql = "SELECT user_oid, name, username, roles, null FROM $tableName WHERE user_oid = ?",
            userOid
        ) ?: throw NoRecordFound(tableName, "User cannot be found")
    }

    /**
     * Attempts to create a new user from the provided [user], returning the new user_oid if successful
     *
     * @throws IllegalArgumentException when the password provided is null
     * @throws [java.sql.SQLException] when the connection throws an error
     */
    fun createUser(connection: Connection, user: RequestUser, userId: Long): Long {
        requireNotNull(user.password) { "User to create must have a non-null password" }
        requireAdmin(connection, userId)
        val sql = """
            INSERT INTO $tableName(name,username,password,roles)
            VALUES(?,?,crypt(?,gen_salt('bf')),ARRAY[${"?,".repeat(user.roles.size).trim(',')}])
            RETURNING user_oid
        """.trimIndent()
        return connection.runReturningFirstOrNull<Long>(
            sql = sql,
            user.name,
            user.username,
            user.password,
            user.roles,
        ) ?: throw NoRecordAffected(tableName, "INSERT statement did not return any data")
    }

    /**
     * Attempts to create a new user from the provided [user], returning the new user_oid if successful
     *
     * @throws IllegalArgumentException when the password provided is null
     * @throws [java.sql.SQLException] when the connection throws an error
     */
    fun updateUser(connection: Connection, user: RequestUser, userId: Long): RequestUser {
        requireNotNull(user.userOid) { "user_oid must not be null" }
        requireAdmin(connection, userId)
        val sql = """
            UPDATE $tableName
            SET    name = ?,
                   username = ?,
                   roles = ARRAY[${"?,".repeat(user.roles.size).trim(',')}]
            WHERE  user_oid = ?
            AND    NOT('admin' = ANY(roles))
            RETURNING user_oid, name, username, roles, null
        """.trimIndent()
        return connection.runReturningFirstOrNull(
            sql = sql,
            user.name,
            user.username,
            user.roles,
            user.userOid,
        ) ?: throw NoRecordAffected(tableName, "No record updated for user_oid = ${user.userOid}")
    }

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

    /** API function to get a list of all users for the application */
    fun getUsers(connection: Connection, userId: Long): List<ResponseUser> {
        requireAdmin(connection, userId)
        val sql = """
            SELECT user_oid, name, username, array_to_string(roles, ', '), NOT('admin' = ANY(roles)) can_edit
            FROM   $tableName
        """.trimIndent()
        return connection.submitQuery(sql = sql)
    }

    /** */
    class UserNotAdmin
        : Throwable("API action requires admin role and the user ID provided does not meet that requirement")

    /** */
    fun requireAdmin(connection: Connection, userId: Long) {
        val isAdmin = connection.queryHasResult(
            sql = "SELECT 1 FROM $tableName WHERE user_oid = ? AND 'admin' = ANY(roles)",
            userId,
        )
        if (!isAdmin) {
            throw UserNotAdmin()
        }
    }
}
