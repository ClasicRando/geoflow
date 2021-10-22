package orm.tables

import database.DatabaseConnection
import kotlinx.serialization.Serializable
import org.ktorm.dsl.*
import org.ktorm.schema.long
import org.ktorm.schema.text
import orm.entities.Action
import kotlin.jvm.Throws

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
object Actions: DbTable<Action>("actions") {
    val actionOid = long("action_oid").primaryKey().bindTo { it.actionOid }
    val state = text("state").bindTo { it.state }
    val role = text("role").bindTo { it.role }
    val name = text("name").bindTo { it.name }
    val description = text("description").bindTo { it.description }
    val href = text("href").bindTo { it.href }

    /**
     * Fields provided when this table is used in the server API to display in a bootstrap table.
     *
     * Each key to the outer map is the field name (or JSON key from the API response), and the inner map is properties
     * of the field (as described here [column-options](https://bootstrap-table.com/docs/api/column-options/)) with the
     * 'data-' prefix automatically added during table HTML creation
     */
    val tableDisplayFields = mapOf(
        "name" to mapOf("name" to "Action"),
        "description" to mapOf(),
    )

    val createSequence = """
        CREATE SEQUENCE public.actions_action_oid_seq
            INCREMENT 1
            START 1
            MINVALUE 1
            MAXVALUE 9223372036854775807
            CACHE 1;
    """.trimIndent()
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
    @Throws(IllegalStateException::class)
    fun userActions(roles: List<String>): List<Record> {
        return DatabaseConnection
            .database
            .from(this)
            .select(name, description, href)
            .whereWithConditions {
                if (!roles.contains("admin"))
                    it += role.inList(roles)
            }
            .map { row ->
                Record(
                    row[name] ?: throw IllegalStateException("name cannot be null"),
                    row[description] ?: throw IllegalStateException("description cannot be null"),
                    row[href] ?: throw IllegalStateException("href cannot be null")
                )
            }
    }
}