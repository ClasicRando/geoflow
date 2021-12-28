package me.geoflow.core.database.tables

import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.functions.GetUserOperations
import me.geoflow.core.database.tables.records.WorkflowOperation
import java.sql.Connection

/**
 * Table used to store the types of workflow states used in generic data pipelines.
 */
object WorkflowOperations : DbTable("workflow_operations"), DefaultData, ApiExposed {

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "name" to mapOf("title" to "Operation"),
    )

    override val defaultRecordsFileName: String = "workflow_operations.csv"

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.workflow_operations
        (
            code text PRIMARY KEY COLLATE pg_catalog."default",
            href text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(href)),
            role text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(role)),
            name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(name)) UNIQUE,
            workflow_group text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(workflow_group)),
            workflow_order integer NOT NULL UNIQUE
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    /**
     * Returns a JSON serializable response of operations available to a user
     *
     * @throws IllegalStateException when either QueryRowSet value is null
     */
    fun userOperations(connection: Connection, userOid: Long): List<WorkflowOperation> {
        val sql = """
            with operations as (
            	select case
            			when workflow_group = 'data' then 'Data Loading'
            			else 'Other'
            		   end "name",
            		   rank() over (partition by workflow_group order by workflow_order) rnk,
            		   href
            	from   ${GetUserOperations.name}(?)
            )
            select name, href
            from   operations
            where  rnk = 1;
        """.trimIndent()
        return connection.submitQuery(sql = sql, userOid)
    }

    /**
     * Returns a JSON serializable response of data operations available to a user
     *
     * @throws IllegalStateException when either QueryRowSet value is null
     */
    fun dataOperations(connection: Connection, userOid: Long): List<WorkflowOperation> {
        val sql = """
            SELECT name, href
            FROM   ${GetUserOperations.name}(?)
            WHERE  workflow_group = 'data'
        """.trimIndent()
        return connection.submitQuery(sql = sql, userOid)
    }
}
