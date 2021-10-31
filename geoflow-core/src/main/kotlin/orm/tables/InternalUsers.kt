package orm.tables

import at.favre.lib.crypto.bcrypt.BCrypt
import database.DatabaseConnection
import org.ktorm.dsl.*
import org.ktorm.schema.long
import org.ktorm.schema.text
import org.ktorm.support.postgresql.textArray
import orm.entities.InternalUser

/**
 * Table used to store users of the web server interface of the application
 *
 * General user definition with username, password (hashed) and roles provided to the user. This table is only used
 * during the user validation/login phase. After that point, the session contains the user information needed
 */
object InternalUsers: DbTable<InternalUser>("internal_users"), SequentialPrimaryKey {
    val userOid = long("user_oid").primaryKey().bindTo { it.userOid }
    val name = text("name").bindTo { it.name }
    val username = text("username").bindTo { it.username }
    val password = text("password").bindTo { it.password }
    val roles = textArray("roles").bindTo { it.roles }

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

    /**
     * Tries to look up the username provided and validate the password if the user can be found.
     *
     * If the user cannot be found or the password is incorrect, the [Failure][ValidationResponse.Failure] object is
     * returned. Otherwise, [Success][ValidationResponse.Success] is returned
     */
    fun validateUser(username: String, password: String): ValidationResponse {
        val user = DatabaseConnection
            .database
            .from(this)
            .select()
            .where(this.username eq username)
            .map(this::createEntity)
            .firstOrNull()
            ?: return ValidationResponse.Failure
        return if (BCrypt.verifyer().verify(password.toCharArray(), user.password).verified) {
            ValidationResponse.Success
        } else {
            ValidationResponse.Failure
        }
    }

    /**
     * Returns the [InternalUser] entity if the username can be found.
     *
     * @throws IllegalArgumentException when the query returns an empty result
     */
    @Throws(IllegalArgumentException::class)
    fun getUser(username: String): InternalUser {
        return DatabaseConnection
            .database
            .from(this)
            .select()
            .where(this.username eq username)
            .map(this::createEntity)
            .firstOrNull() ?: throw IllegalArgumentException("User cannot be found")
    }
}
