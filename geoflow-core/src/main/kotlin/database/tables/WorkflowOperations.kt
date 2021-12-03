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

    /** API response data class for JSON serialization */
    @Serializable
    data class WorkflowOperation(
        /** workflow operation name */
        val name: String,
        /** endpoint of workflow operation on server */
        val href: String,
    )

    /**
     * Returns a JSON serializable response of operations available to a user
     *
     * @throws IllegalStateException when either QueryRowSet value is null
     */
    fun userOperations(connection: Connection, userOid: Long): List<WorkflowOperation> {
        val sql = """
            WITH user_roles AS (
                SELECT REGEXP_REPLACE(unnest(roles),'admin',null) "role"
                FROM   ${InternalUsers.tableName}
                WHERE  user_oid = ?
            )
            SELECT name, href
            FROM   $tableName t1, user_roles t2
            WHERE  t1.role = COALESCE(t2.role,t1.role)
            ORDER BY workflow_order
        """.trimIndent()
        return connection.submitQuery(sql = sql, userOid)
    }
}
