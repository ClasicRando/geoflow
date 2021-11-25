package database.tables

import database.NoRecordAffected
import database.extensions.getList
import database.extensions.queryFirstOrNull
import database.extensions.runReturningFirstOrNull
import database.extensions.runUpdate
import database.extensions.submitQuery
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.sql.ResultSet

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
            username text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(username)),
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
    @QueryResultRecord
    class InternalUser private constructor(
        /** unique id of the user */
        val userOid: Long,
        /** full name of the user */
        val name: String,
        /** public username */
        val username: String,
        /** hashed password of the user */
        private val password: String,
        /** list of roles of the user. Provides access to endpoints or abilities in the api */
        val roles: List<String>,
    ) {
        @Suppress("UNUSED")
        companion object {
            /** SQL query used to generate the parent class */
            val sql: String = "SELECT * FROM $tableName WHERE username = ?"
            private const val USER_OID = 1
            private const val NAME = 2
            private const val USERNAME = 3
            private const val PASSWORD = 4
            private const val ROLES = 5
            /** Function to extract a row from a [ResultSet] to get a result record */
            fun fromResultSet(rs: ResultSet): InternalUser {
                return InternalUser(
                    rs.getLong(USER_OID),
                    rs.getString(NAME),
                    rs.getString(USERNAME),
                    rs.getString(PASSWORD),
                    rs.getArray(ROLES).getList(),
                )
            }
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
     * @throws IllegalArgumentException when the query returns an empty result
     * @throws [java.sql.SQLException] when the connection throws an error
     */
    fun getUser(connection: Connection, username: String): InternalUser {
        return connection.queryFirstOrNull(
            sql = InternalUser.sql,
            username
        ) ?: throw IllegalArgumentException("User cannot be found")
    }

    /** API request to */
    @Serializable
    data class RequestUser(
        /** */
        @SerialName("user_oid")
        val userOid: Long? = null,
        /** */
        @SerialName("fullName")
        val name: String,
        /** */
        @SerialName("username")
        val username: String,
        /** */
        @SerialName("roles")
        val roles: List<String>,
        /** */
        @SerialName("password")
        val password: String?,
    )

    /**
     * Attempts to create a new user from the provided [user], returning the new user_oid if successful
     *
     * @throws IllegalArgumentException when the password provided is null
     * @throws [java.sql.SQLException] when the connection throws an error
     */
    fun createUser(connection: Connection, user: RequestUser): Long? {
        requireNotNull(user.password) { "User to create must have a non-null password" }
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
        )
    }

    /**
     * Attempts to create a new user from the provided [user], returning the new user_oid if successful
     *
     * @throws IllegalArgumentException when the password provided is null
     * @throws [java.sql.SQLException] when the connection throws an error
     */
    fun updateUser(connection: Connection, user: RequestUser): Int {
        requireNotNull(user.userOid) { "user_oid must not be null" }
        val sql = """
            UPDATE $tableName
            SET    name = ?,
                   username = ?,
                   roles = ARRAY[${"?,".repeat(user.roles.size).trim(',')}]
            WHERE  user_oid = ?
        """.trimIndent()
        return connection.runUpdate(
            sql = sql,
            user.name,
            user.username,
            user.roles,
            user.userOid,
        )
    }

    /**
     * Attempts to create a new user from the provided [user], returning the new user_oid if successful
     *
     * @throws IllegalArgumentException when the password provided is null
     * @throws [java.sql.SQLException] when the connection throws an error
     */
    fun updateUserV2(connection: Connection, user: RequestUser): RequestUser {
        requireNotNull(user.userOid) { "user_oid must not be null" }
        val sql = """
            UPDATE $tableName
            SET    name = ?,
                   username = ?,
                   roles = ARRAY[${"?,".repeat(user.roles.size).trim(',')}]
            WHERE  user_oid = ?
            RETURNING name, username, roles, user_oid
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
    data class User(
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
    fun getUsers(connection: Connection, userOid: Long): List<User> {
        val sql = """
            SELECT user_oid, name, username, array_to_string(roles, ', '),
                   CASE WHEN user_oid != ? AND 'admin' = ANY(roles) THEN false ELSE true END can_edit
            FROM   $tableName
        """.trimIndent()
        return connection.submitQuery(sql = sql, userOid)
    }
}
