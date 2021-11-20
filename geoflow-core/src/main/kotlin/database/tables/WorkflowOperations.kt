package database.tables

import database.extensions.submitQuery
import kotlinx.serialization.Serializable
import java.sql.Connection

/**
 * Table used to store the types of workflow states used in generic data pipelines.
 */
object WorkflowOperations : DbTable("workflow_operations"), DefaultData, ApiExposed {

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "name" to mapOf("name" to "Operation"),
    )

    override val defaultRecordsFileName: String = "workflow_operations.csv"

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.workflow_operations
        (
            code text PRIMARY KEY COLLATE pg_catalog."default",
            href text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(href)),
            role text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(role)),
            name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(name)) UNIQUE,
            workflow_order integer NOT NULL UNIQUE
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    /**
     * API response data class for JSON serialization
     *
     * @param name workflow operation name
     * @param href endpoint of workflow operation on server
     */
    @Serializable
    data class Record(val name: String, val href: String)

    /**
     * Returns a JSON serializable response of operations available to a user
     *
     * @throws IllegalStateException when either QueryRowSet value is null
     */
    fun userOperations(connection: Connection, roles: List<String>): List<Record> {
        val whereClause = if ("admin" !in roles) {
            " WHERE $tableName.role in (${"?,".repeat(roles.size).trim(',')})"
        } else ""
        val sql = """
            SELECT $tableName.name, $tableName.href
            FROM $tableName
            $whereClause
            ORDER BY $tableName.workflow_order
        """.trimIndent()
        return connection.submitQuery(sql = sql, parameters = roles.minus("admin"))
    }
}
