package database.tables

import database.extensions.submitQuery
import kotlinx.serialization.Serializable
import java.sql.Connection

/**
 * Table used to store the available roles that a user can hold
 */
object Roles : DbTable("roles"), DefaultData {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.roles
        (
            name text PRIMARY KEY COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(name)),
            description text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(description))
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val defaultRecordsFileName: String = "roles.csv"

    /** Data class to represent a single database record */
    @Serializable
    data class Role(
        /** name of role */
        val name: String,
        /** description of role */
        val description: String,
    )

    /**
     * Returns a list of all roles currently available to users
     */
    fun getRecords(connection: Connection): List<Role> {
        return connection.submitQuery("SELECT name, description FROM $tableName")
    }
}
