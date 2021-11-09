package database.tables

import database.DatabaseConnection
import kotlinx.serialization.Serializable
import orm.tables.ApiExposed
import orm.tables.DefaultData
import rowToClass

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
    suspend fun userOperations(roles: List<String>): List<Record> = DatabaseConnection.queryConnection { connection ->
        val whereClause = if ("admin" !in roles) {
            " WHERE $tableName.role in (${"?,".repeat(roles.size).trim(',')})"
        } else ""
        val statement = connection.prepareStatement(
            """
                SELECT $tableName.name, $tableName.href
                FROM $tableName
                $whereClause
                ORDER BY $tableName.workflow_order
            """.trimIndent()
        )
        for (role in roles.minus("admin").withIndex()) {
            statement.setString(role.index + 1, role.value)
        }
        statement.use {
            it.executeQuery().use { rs ->
                generateSequence {
                    if (rs.next()) rs.rowToClass<Record>() else null
                }.toList()
            }
        }
    }
}
