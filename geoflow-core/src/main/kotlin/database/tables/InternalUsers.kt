package database.tables

import at.favre.lib.crypto.bcrypt.BCrypt
import database.DatabaseConnection
import orm.tables.SequentialPrimaryKey

/**
 * Table used to store users of the web server interface of the application
 *
 * General user definition with username, password (hashed) and roles provided to the user. This table is only used
 * during the user validation/login phase. After that point, the session contains the user information needed
 */
object InternalUsers: DbTable("internal_users"), SequentialPrimaryKey {

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.internal_users
        (
            user_oid bigint NOT NULL DEFAULT nextval('internal_users_user_oid_seq'::regclass),
            name text COLLATE pg_catalog."default" NOT NULL,
            username text COLLATE pg_catalog."default" NOT NULL,
            password text COLLATE pg_catalog."default" NOT NULL,
            roles text[] COLLATE pg_catalog."default" NOT NULL,
            CONSTRAINT internal_users_pkey PRIMARY KEY (user_oid)
        )
        WITH (
            OIDS = FALSE
        )
    """.trimIndent()

    /** Validation response as a sealed interface of success or failure (standard message) */
    sealed interface ValidationResponse {
        object Success : ValidationResponse
        object Failure: ValidationResponse {
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
     */
    suspend fun validateUser(username: String, password: String): ValidationResponse {
        return DatabaseConnection.queryConnectionSingle { connection ->
            connection.prepareStatement(
                "SELECT password FROM $tableName WHERE username = ?"
            ).use { statement ->
                statement.setString(1, username)
                statement.executeQuery().use { rs ->
                    if (rs.next()) rs.getString(1) else null
                }
            }?.let { userPassword ->
                if (BCrypt.verifyer().verify(password.toCharArray(), userPassword).verified) {
                    ValidationResponse.Success
                } else {
                    ValidationResponse.Failure
                }
            } ?: ValidationResponse.Failure
        }
    }

    /**
     * Returns the [InternalUser] entity if the username can be found.
     *
     * @throws IllegalArgumentException when the query returns an empty result
     */
    suspend fun getUser(username: String): InternalUser {
        return DatabaseConnection.queryConnectionSingle { connection ->
            connection.prepareStatement(
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
                            (rs.getArray(5).array as Array<*>).mapNotNull {
                                if (it is String) it else null
                            }
                        )
                    } else {
                        null
                    }
                }
            } ?: throw IllegalArgumentException("User cannot be found")
        }
    }
}
