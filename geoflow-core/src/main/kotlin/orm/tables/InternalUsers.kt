package orm.tables

import at.favre.lib.crypto.bcrypt.BCrypt
import database.DatabaseConnection
import org.ktorm.dsl.*
import org.ktorm.schema.long
import org.ktorm.schema.Table
import org.ktorm.schema.text
import org.ktorm.support.postgresql.textArray
import orm.entities.InternalUser

object InternalUsers: Table<InternalUser>("internal_users") {
    val userOid = long("user_oid").primaryKey().bindTo { it.userOid }
    val name = text("name").bindTo { it.name }
    val username = text("username").bindTo { it.username }
    val password = text("password").bindTo { it.password }
    val roles = textArray("roles").bindTo { it.roles }

    val createSequence = """
        CREATE SEQUENCE public.internal_users_user_oid_seq
            INCREMENT 1
            START 1
            MINVALUE 1
            MAXVALUE 9223372036854775807
            CACHE 1;
    """.trimIndent()

    val createStatement = """
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

    data class ValidationResponse(val isSuccess: Boolean, val message: String = "")

    fun validateUser(username: String, password: String): ValidationResponse {
        val user = DatabaseConnection
            .database
            .from(this)
            .select()
            .where(this.username eq username)
            .map(this::createEntity)
            .firstOrNull() ?: return ValidationResponse(false, "Incorrect username or password")
        val verified = BCrypt.verifyer().verify(password.toCharArray(), user.password).verified
        return ValidationResponse(verified, if (verified) "" else "Incorrect username or password")
    }

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