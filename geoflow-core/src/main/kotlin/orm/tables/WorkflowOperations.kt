package orm.tables

import database.DatabaseConnection
import kotlinx.serialization.Serializable
import org.ktorm.dsl.*
import org.ktorm.schema.int
import org.ktorm.schema.text
import orm.entities.WorkflowOperation
import kotlin.jvm.Throws

/**
 * Table used to store the types of workflow states used in generic data pipelines.
 */
object WorkflowOperations: DbTable<WorkflowOperation>("workflow_operations"), DefaultData, ApiExposed {
    val code = text("code").primaryKey().bindTo { it.code }
    val href = text("href").bindTo { it.href }
    val role = text("role").bindTo { it.role }
    val name = text("name").bindTo { it.name }
    val workflowOrder = int("workflow_order").bindTo { it.workflowOrder }

    override val tableDisplayFields = mapOf(
        "name" to mapOf("name" to "Operation"),
    )

    override val defaultRecordsFileName: String = "workflow_operations.csv"

    override val createStatement = """
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

    /** API response data class for JSON serialization */
    @Serializable
    data class Record(val name: String, val href: String)

    /**
     * Returns a JSON serializable response of operations available to a user
     *
     * @throws IllegalStateException when either QueryRowSet value is null
     */
    @Throws(IllegalStateException::class)
    fun userOperations(roles: List<String>): List<Record> {
        return DatabaseConnection
            .database
            .from(this)
            .select(name, href)
            .whereWithConditions {
                if (!roles.contains("admin"))
                    it += role.inList(roles)
            }
            .orderBy(workflowOrder.asc())
            .map { row ->
                Record(
                    row[name] ?: throw IllegalStateException("name cannot be null"),
                    row[href] ?: throw IllegalStateException("href cannot be null")
                )
            }
    }
}