package me.geoflow.core.database.tables

import me.geoflow.core.database.extensions.runUpdate
import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.functions.UserHasRun
import me.geoflow.core.database.tables.records.PlottingMethod
import me.geoflow.core.utils.requireNotEmpty
import java.sql.Connection

/**
 * Table used to store the ordered plotting methods for a given pipeline run. References a source table for plotting
 * (using a file_id in [SourceTables]) and a plotting method (using method_id from [PlottingMethodTypes])
 */
object PlottingMethods : DbTable("plotting_methods"), Triggers, ApiExposed {

    override val createStatement: String = """
		CREATE TABLE IF NOT EXISTS public.plotting_methods
        (
            run_id bigint NOT NULL REFERENCES public.pipeline_runs (run_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
            plotting_order smallint NOT NULL,
            method_type integer NOT NULL REFERENCES public.plotting_method_types (method_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
            file_id text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(file_id)),
            CONSTRAINT plotting_methods_pkey PRIMARY KEY (run_id, plotting_order),
            CONSTRAINT plotting_methods_st_fkey FOREIGN KEY (file_id, run_id)
                REFERENCES public.source_tables (file_id, run_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "order" to mapOf(),
        "method_type" to mapOf("title" to "Name", "formatter" to "methodTypeFormatter"),
        "file_id" to mapOf("title" to "File ID"),
    )

    override val triggers: List<Trigger> = listOf(
        Trigger(
            trigger = """
                CREATE TRIGGER set_records_trigger
                    AFTER INSERT
                    ON public.plotting_methods
                    REFERENCING NEW TABLE AS new_table
                    FOR EACH STATEMENT
                    EXECUTE FUNCTION public.plotting_method_insert();
            """.trimIndent(),
            triggerFunction = """
                CREATE OR REPLACE FUNCTION public.plotting_method_insert()
                    RETURNS trigger
                    LANGUAGE 'plpgsql'
                    COST 100
                    VOLATILE NOT LEAKPROOF
                AS ${'$'}BODY${'$'}
                declare
                    check_count bigint;
                begin
                    select count(distinct run_id)
                    into   check_count
                    from   new_table;
                    
                    if check_count > 1 then
                        raise exception 'Cannot insert for multiple run_id';
                    end if;
                    
                    select count(0)
                    into   check_count
                    from  (select plotting_order, row_number() over (order by plotting_order) rn
                           from   new_table) t1
                    where  rn != plotting_order;
                    
                    if check_count > 0 then
                        raise exception 'The order of the plotting methods has skipped a value';
                    end if;
                    return null;
                end;
                ${'$'}BODY${'$'};
            """.trimIndent()
        ),
    )

    /** Returns a list of [PlottingMethod] records for the given [runId] */
    fun getRecords(connection: Connection, runId: Long): List<PlottingMethod> {
        return connection.submitQuery(
            sql = "SELECT * FROM $tableName WHERE run_id = ? ORDER BY plotting_order",
            runId,
        )
    }

    /**
     * Attempts to set the plotting methods for a given runId using the list of [methods] provided. Performs a complete
     * replace of current methods. Returns delete and insert counts as a [Pair].
     *
     * This method should always be called from an open transaction (i.e. connection without autocommit) to avoid
     * deleting records without a rollback available if the new records do not pass the checks/assertions.
     *
     * @throws IllegalStateException when [methods] is empty
     * @throws me.geoflow.core.database.errors.IllegalUserAction when the user does not have the privileges required
     * @throws java.sql.SQLException when the connection throws an exception. Usually means a constraint or trigger
     * failed
     */
    fun setRecords(
        connection: Connection,
        userOid: Long,
        runId: Long,
        methods: List<PlottingMethod>,
    ): Pair<Int, Int> {
        requireNotEmpty(methods) { "Methods provided must be a non-empty list" }
        UserHasRun.requireUserRun(connection, userOid, runId)
        val deleteCount = connection.runUpdate(
            sql = "DELETE FROM $tableName WHERE run_id = ?",
            runId
        )
        val insertCount = connection.runUpdate(
            sql = """
                INSERT INTO $tableName(run_id,plotting_order,method_type,file_id)
                VALUES${"(?,?,?,?),".repeat(methods.size).trimEnd(',')}
            """.trimIndent(),
            methods.flatMap { listOf(it.runId, it.order, it.methodType, it.fileId) }
        )
        return deleteCount to insertCount
    }

}
