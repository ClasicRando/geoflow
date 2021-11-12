package database.tables

import database.submitQuery
import kotlinx.serialization.Serializable
import java.sql.Connection

/**
 * Table used to store the types of workflow states used in generic data pipelines.
 */
object WorkflowOperations: DbTable("workflow_operations"), DefaultData, ApiExposed {

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
        return connection.submitQuery(sql = sql, *roles.minus("admin").toTypedArray())
    }
}
