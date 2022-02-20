package me.geoflow.core.database.tables

import me.geoflow.core.database.errors.NoRecordAffected
import me.geoflow.core.database.errors.NoRecordFound
import me.geoflow.core.database.extensions.queryFirstOrNull
import me.geoflow.core.database.extensions.runReturningFirstOrNull
import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.functions.UserHasRun
import me.geoflow.core.database.tables.records.ColumnComparison
import me.geoflow.core.database.tables.records.SourceTableColumn
import me.geoflow.core.database.tables.records.SourceTableColumnUpdate
import java.sql.Connection

/**
 * Table used to store the metadata of the columns found in the files/tables from [SourceTables]
 *
 * When a source file is analyzed the column metadata is inserted into this table to alert a user if the file has
 * changed in the columns provided or the character length of data.
 */
object SourceTableColumns : DbTable("source_table_columns"), ApiExposed, Triggers {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.source_table_columns
        (
            stc_oid bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            st_oid bigint NOT NULL REFERENCES public.source_tables (st_oid) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
            name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(name)),
            type text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(type)),
            max_length integer NOT NULL,
            min_length integer NOT NULL,
            label text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(label)),
            column_index integer NOT NULL,
            report_group integer CHECK(report_group <> 0),
            CONSTRAINT column_name_table UNIQUE (st_oid, name)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val triggers: List<Trigger> = listOf(
        Trigger(
            trigger = """
                CREATE TRIGGER check_stc_update
                    BEFORE UPDATE OF report_group
                    ON public.source_table_columns
                    FOR EACH ROW
                    EXECUTE FUNCTION public.check_source_column();
            """.trimIndent(),
            triggerFunction = """
                CREATE OR REPLACE FUNCTION public.check_source_column()
                    RETURNS trigger
                    LANGUAGE 'plpgsql'
                    COST 100
                    VOLATILE NOT LEAKPROOF
                AS ${'$'}BODY${'$'}
                DECLARE
                    check_report_group int;
                BEGIN
                    SELECT COUNT(0)
                    INTO   check_report_group
                    FROM  (SELECT report_group, st_oid
                           FROM   source_table_columns
                           UNION
                           SELECT report_group, st_oid
                           FROM   generated_table_columns) t
                    WHERE  report_group = NEW.report_group
                    AND    st_oid != NEW.st_oid;
                    ASSERT check_report_group = 0, 'report_group value belongs to another source_table';
                    RETURN NEW;
                END;
                ${'$'}BODY${'$'};
            """.trimIndent()
        ),
    )

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "name" to mapOf(),
        "type" to mapOf(),
        "max_length" to mapOf(),
        "min_length" to mapOf(),
        "label" to mapOf(),
    )

    /** Returns a list of [SourceTableColumn] records for the specified [stOid] */
    fun getRecords(connection: Connection, stOid: Long): List<SourceTableColumn> {
        return connection.submitQuery(sql = "SELECT * FROM $tableName WHERE st_oid = ?", stOid)
    }

    /** */
    val columnComparisonFields: Map<String, Map<String, String>> = mapOf(
        "current_name" to mapOf(),
        "current_type" to mapOf(),
        "current_max_length" to mapOf(),
        "last_name" to mapOf(),
        "last_type" to mapOf(),
        "last_max_length" to mapOf(),
    )

    /** */
    fun getComparison(connection: Connection, stOid: Long): List<ColumnComparison> {
        val lastStOid = connection.queryFirstOrNull<Long>(
            sql = """
                WITH last_run AS (
                    SELECT t3.run_id
                    FROM   ${PipelineRuns.tableName} t1
                    JOIN   ${SourceTables.tableName} t2
                    ON     t1.run_id = t2.run_id
                    LEFT JOIN ${PipelineRuns.tableName} t3
                    ON     t1.ds_id = t3.ds_id
                    WHERE  t2.st_oid = ?
                    ORDER BY 1 DESC
                    LIMIT  1 OFFSET 1
                ), current_file_id AS (
                    SELECT file_id
                    FROM   ${SourceTables.tableName}
                    WHERE  st_oid = ?
                )
                SELECT st_oid
                FROM   ${SourceTables.tableName} t1
                JOIN   last_run t2
                ON     t1.run_id = t2.run_id
                JOIN   current_file_id t3
                ON     t1.file_id = t3.file_id
            """.trimIndent(),
            stOid,
        )
        return connection.submitQuery(
            sql = """
                SELECT t1.name, t1.type, t1.max_length, t2.name, t2.type, t2.max_length
                FROM  (SELECT * FROM $tableName WHERE st_oid = ?) t1
                FULL JOIN (SELECT * FROM $tableName WHERE st_oid = ?) t2
                ON     t1.name = t2.name
            """.trimIndent(),
            stOid,
            lastStOid,
        )
    }

    /** Updates the label and report_group of a source table column */
    fun updateRecord(connection: Connection, userId: Long, columnUpdate: SourceTableColumnUpdate): SourceTableColumn {
        val runId = connection.queryFirstOrNull<Long>(
            sql = """
                SELECT DISTINCT st.run_id
                FROM   $tableName stc
                JOIN   ${SourceTables.tableName} st
                ON     stc.st_oid = st.st_oid
                WHERE  stc.stc_oid = ?
            """.trimIndent(),
            columnUpdate.stcOid,
        ) ?: throw NoRecordFound(tableName, "Could not find a run_id for stc_oid = '${columnUpdate.stcOid}'")
        UserHasRun.requireUserRun(connection, userId, runId)
        return connection.runReturningFirstOrNull(
            sql = """
                UPDATE $tableName
                SET    label = ?,
                       report_group = ?
                WHERE  stc_oid = ?
                RETURNING *
            """.trimIndent(),
            columnUpdate.label,
            columnUpdate.reportGroup,
            columnUpdate.stcOid,
        ) ?: throw NoRecordAffected(tableName, "Did not update record for stc_oid = '${columnUpdate.stcOid}'")
    }

}
