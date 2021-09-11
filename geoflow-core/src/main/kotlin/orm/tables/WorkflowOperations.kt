package orm.tables

import database.DatabaseConnection
import kotlinx.serialization.Serializable
import org.ktorm.dsl.*
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.text
import orm.entities.WorkflowOperation
import kotlin.jvm.Throws

object WorkflowOperations: Table<WorkflowOperation>("workflow_operations") {
    val code = text("code").primaryKey().bindTo { it.code }
    val href = text("href").bindTo { it.href }
    val role = text("role").bindTo { it.role }
    val name = text("name").bindTo { it.name }
    val workflowOrder = int("workflow_order").bindTo { it.workflowOrder }

    val tableDisplayFields = mapOf("name" to mapOf<String, String>())

    val createStatement = """
        CREATE TABLE IF NOT EXISTS public.workflow_operations
        (
            code text COLLATE pg_catalog."default" NOT NULL,
            href text COLLATE pg_catalog."default" NOT NULL,
            role text COLLATE pg_catalog."default" NOT NULL,
            name text COLLATE pg_catalog."default" NOT NULL,
            workflow_order integer NOT NULL,
            CONSTRAINT workflow_operations_pkey PRIMARY KEY (code)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    @Serializable
    data class Record(val name: String, val href: String)

    @Throws(IllegalArgumentException::class)
    fun userOperations(roles: List<String>): List<Record> {
        return DatabaseConnection
            .database
            .from(this)
            .select(name, href)
            .whereWithConditions {
                if (!roles.contains("admin"))
                    it += Actions.role.inList(roles)
            }
            .map { row ->
                Record(
                    row[name] ?: throw IllegalArgumentException("name cannot be null"),
                    row[href] ?: throw IllegalArgumentException("href cannot be null")
                )
            }
    }

    fun workflowName(workflowCode: String): String {
        return DatabaseConnection
            .database
            .from(this)
            .select(name)
            .where(code eq workflowCode)
            .map { row -> row[name] }
            .firstOrNull() ?: ""
    }
}