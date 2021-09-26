package orm.tables

import database.DatabaseConnection
import kotlinx.serialization.Serializable
import org.ktorm.dsl.*
import org.ktorm.schema.Table
import org.ktorm.schema.long
import org.ktorm.schema.text
import orm.entities.Action
import kotlin.jvm.Throws

object Actions: Table<Action>("actions") {
    val actionOid = long("action_oid").primaryKey().bindTo { it.actionOid }
    val state = text("state").bindTo { it.state }
    val role = text("role").bindTo { it.role }
    val name = text("name").bindTo { it.name }
    val description = text("description").bindTo { it.description }
    val href = text("href").bindTo { it.href }

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
    val createStatement = """
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

    @Serializable
    data class Record(val name: String, val description: String, val href: String)

    @Throws(IllegalArgumentException::class)
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
                    row[name] ?: throw IllegalArgumentException("name cannot be null"),
                    row[description] ?: throw IllegalArgumentException("description cannot be null"),
                    row[href] ?: throw IllegalArgumentException("href cannot be null")
                )
            }
    }
}