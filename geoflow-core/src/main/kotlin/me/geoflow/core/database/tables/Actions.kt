package me.geoflow.core.database.tables

import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.functions.GetUserActions
import me.geoflow.core.database.tables.records.Action
import java.sql.Connection

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
object Actions : DbTable("actions"), ApiExposed, DefaultData {

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "name" to mapOf("title" to "Action"),
        "description" to mapOf(),
    )

    override val createStatement: String = """
		CREATE TABLE IF NOT EXISTS public.actions
        (
			action_id bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY,
            state text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(state)),
            role text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(role))
                REFERENCES roles (name) MATCH SIMPLE
				ON DELETE RESTRICT
				ON UPDATE CASCADE,
            name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(name)),
            description text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(description)),
            href text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(href))
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val defaultRecordsFileName: String = "actions.csv"

    /**
     * API function to get a list of all user actions based upon the [userId] of the current user
     */
    fun userActions(connection: Connection, userId: Long): List<Action> {
        val sql = """
            SELECT name, description, href
            FROM   ${GetUserActions.name}(?)
        """.trimIndent()
        return connection.submitQuery(sql = sql, userId)
    }
}
