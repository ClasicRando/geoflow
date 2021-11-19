package database.tables

import at.favre.lib.crypto.bcrypt.BCrypt
import database.extensions.getList
import database.extensions.queryFirstOrNull
import database.extensions.runReturningFirstOrNull
import requireNotEmpty
import java.sql.Connection

/**
 * Table used to store users of the web server interface of the application
 *
 * General user definition with username, password (hashed) and roles provided to the user. This table is only used
 * during the user validation/login phase. After that point, the session contains the user information needed
 */
object InternalUsers : DbTable("internal_users") {

    override val createStatement = """
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
        object Success : ValidationResponse
        object Failure : ValidationResponse {
            const val ERROR_MESSAGE = "Incorrect username or password"
        }
    }

    data class InternalUser(
        val userOid: Long,
        val name: String,
        val username: String,
        val password: String,
        val roles: List<String>,
    )

    /**
     * Tries to look up the username provided and validate the password if the user can be found.
     *
     * If the user cannot be found or the password is incorrect, the [Failure][ValidationResponse.Failure] object is
     * returned. Otherwise, [Success][ValidationResponse.Success] is returned
     *
     * @throws [java.sql.SQLException] when the connection throws an error
     */
    fun validateUser(connection: Connection, username: String, password: String): ValidationResponse {
        val sql = "SELECT password FROM $tableName WHERE username = ?"
        return connection.queryFirstOrNull<String>(sql, username)?.let { userPassword ->
            if (BCrypt.verifyer().verify(password.toCharArray(), userPassword).verified) {
                ValidationResponse.Success
            } else {
                ValidationResponse.Failure
            }
        } ?: ValidationResponse.Failure
    }

    /**
     * Returns the [InternalUser] entity if the username can be found.
     *
     * @throws IllegalArgumentException when the query returns an empty result
     * @throws [java.sql.SQLException] when the connection throws an error
     */
    fun getUser(connection: Connection, username: String): InternalUser {
        return connection.prepareStatement(
            "SELECT * FROM $tableName WHERE username = ?"
        ).use { statement ->
            statement.setString(1, username)
            statement.executeQuery().use { rs ->
                if (rs.next()) {
                    InternalUser(
                        rs.getLong(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getArray(5).getList(),
                    )
                } else {
                    null
                }
            }
        } ?: throw IllegalArgumentException("User cannot be found")
    }

    /**
     * Attempts to create a new user from the provided [params], returning the new user_oid if successful
     *
     * @throws IllegalArgumentException when various requirements are not met, such as
     * - [params] does not contain fullName, username, roles or password
     * - fullName, username or password lists do not contain exactly 1 element
     * - roles is empty
     * @throws [java.sql.SQLException] when the connection throws an error
     */
    fun createUser(connection: Connection, params: Map<String, List<String>>): Long? {
        val fullName = params["fullName"] ?: throw IllegalArgumentException("New user must have a name provided")
        val username = params["username"] ?: throw IllegalArgumentException("New user must have a username provided")
        val roles = params["roles"] ?: throw IllegalArgumentException("New user must have roles provided")
        val password = params["password"] ?: throw IllegalArgumentException("New user must have a password provided")
        require(fullName.size == 1) { "New user must have a single name provided" }
        require(username.size == 1) { "New user must have a single username provided" }
        requireNotEmpty(roles) { "New user must have 1 or more role" }
        require(password.size == 1) { "New user must have a single password provided" }
        val sql = """
            INSERT INTO $tableName(name,username,password,roles)
            VALUES(?,?,crypt(?,gen_salt('bf')),ARRAY[${"?,".repeat(roles.size).trim(',')}])
            RETURNING user_oid
        """.trimIndent()
        return connection.runReturningFirstOrNull<Long>(
            sql = sql,
            fullName[0],
            username[0],
            password[0],
            *roles.toTypedArray(),
        )
    }
}
