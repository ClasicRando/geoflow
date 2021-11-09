package database.tables

import database.DatabaseConnection
import kotlinx.serialization.Serializable
import orm.tables.ApiExposed
import orm.tables.SequentialPrimaryKey

/**
 * Table used to store actions available to users with certain roles.
 *
 * Actions within the application mostly revolve around a user performing meta operations on the core data within the
 * database. For example, creating a user of the application would be an action that is associated to a specific role.
 * The base application includes some stock actions associated with user roles, but future implementation can expand to
 * more roles and more actions per role as needed.
 *
 * The major utility of actions is in linking that action to a server endpoint (stored in href) to perform or facilitate
 * the desired action. To continue the example above, the 'Create User' action is linked to an endpoint that provides a
 * form interface for the current user to create another user. These endpoints should include role validation so that a
 * user without the required role is given an error status page.
 */
object Actions: DbTable("actions"), ApiExposed, SequentialPrimaryKey {

    /**
     * Fields provided when this table is used in the server API to display in a bootstrap table.
     *
     * Each key to the outer map is the field name (or JSON key from the API response), and the inner map is properties
     * of the field (as described here [column-options](https://bootstrap-table.com/docs/api/column-options/)) with the
     * 'data-' prefix automatically added during table HTML creation
     */
    override val tableDisplayFields = mapOf(
        "name" to mapOf("name" to "Action"),
        "description" to mapOf(),
    )

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.actions
        (
            state text COLLATE pg_catalog."default" NOT NULL,
            role text COLLATE pg_catalog."default" NOT NULL,
            name text COLLATE pg_catalog."default" NOT NULL,
            description text COLLATE pg_catalog."default" NOT NULL,
            href text COLLATE pg_catalog."default" NOT NULL,
            action_oid bigint NOT NULL DEFAULT nextval('actions_action_oid_seq'::regclass),
            CONSTRAINT actions_pkey PRIMARY KEY (action_oid)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    /** API response data class for JSON serialization */
    @Serializable
    data class Record(val name: String, val description: String, val href: String)

    /**
     * API function to get a list of all user actions based upon the [roles] of the current user
     *
     * @throws IllegalStateException When any row item is null
     */
    suspend fun userActions(roles: List<String>): List<Record> {
        val whereClause = if ("admin" !in roles) {
            " WHERE $tableName.role in (${"?,".repeat(roles.size).trim(',')})"
        } else ""
        val sql = "SELECT $tableName.name, $tableName.description, $tableName.href FROM $tableName$whereClause"
        return DatabaseConnection.submitQuery(sql = sql, parameters = roles.minus("admin"))
    }
}
