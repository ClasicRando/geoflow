package me.geoflow.core.database.tables

import me.geoflow.core.database.errors.NoRecordAffected
import me.geoflow.core.database.errors.NoRecordFound
import me.geoflow.core.database.extensions.executeNoReturn
import me.geoflow.core.database.extensions.queryFirstOrNull
import me.geoflow.core.database.extensions.runReturningFirstOrNull
import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.functions.UserHasRun
import me.geoflow.core.database.tables.records.GeneratedTableColumn
import java.sql.Connection

/** */
object GeneratedTableColumns : DbTable("generated_table_columns"), ApiExposed {

    @Suppress("MaxLineLength")
    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.generated_table_columns
        (
            gtc_oid bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            st_oid bigint NOT NULL REFERENCES public.source_tables (st_oid) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
            generation_expression text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(generation_expression)),
            name text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(name)),
            label text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(label)),
            report_group integer NOT NULL CHECK(report_group <> 0),
            CONSTRAINT column_name_table UNIQUE (st_oid, name)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "name" to mapOf(),
        "generation_expression" to mapOf("title" to "Expression"),
        "label" to mapOf(),
        "report_group" to mapOf(),
        "action" to mapOf("formatter" to "generatedFieldAction")
    )

    /** */
    fun getRecords(connection: Connection, stOid: Long): List<GeneratedTableColumn> {
        return connection.submitQuery(
            sql = "SELECT * FROM $tableName WHERE st_oid = ?",
            stOid,
        )
    }

    /** */
    fun createRecord(connection: Connection, userOid: Long, generatedColumn: GeneratedTableColumn): Long {
        val runId = SourceTables.getRunId(connection, generatedColumn.stOid)
        UserHasRun.requireUserRun(connection, userOid, runId)
        return connection.runReturningFirstOrNull(
            sql = """
                INSERT INTO $tableName(st_oid,generation_expression,name,label,report_group)
                VALUES(?,?,?,?,?)
                RETURNING gtc_oid
            """.trimIndent(),
            generatedColumn.stOid,
            generatedColumn.expression,
            generatedColumn.name,
            generatedColumn.label,
            generatedColumn.reportGroup,
        ) ?: throw NoRecordAffected(
            tableName,
            "Could not created a new generated column (${generatedColumn.name})"
        )
    }

    /** */
    fun updateRecord(
        connection: Connection,
        userOid: Long,
        generatedColumn: GeneratedTableColumn,
    ): GeneratedTableColumn {
        val runId = SourceTables.getRunId(connection, generatedColumn.stOid)
        UserHasRun.requireUserRun(connection, userOid, runId)
        return connection.runReturningFirstOrNull(
            sql = """
                UPDATE $tableName
                SET    generation_expression = ?,
                       name = ?,
                       label = ?,
                       report_group = ?
                WHERE  gtc_oid = ?
            """.trimIndent(),
            generatedColumn.expression,
            generatedColumn.name,
            generatedColumn.label,
            generatedColumn.reportGroup,
            generatedColumn.gtcOid,
        ) ?: throw NoRecordAffected(
            tableName,
            "Error during record update or record does not exist for gtc_oid = ${generatedColumn.gtcOid}"
        )
    }

    /** */
    fun deleteRecord(
        connection: Connection,
        userOid: Long,
        gtcOid: Long,
    ) {
        val runId = connection.queryFirstOrNull<Long>(
            sql = """
                SELECT st.run_id
                FROM   $tableName gtc
                JOIN   ${SourceTables.tableName} st
                ON     gtc.st_oid = st.st_oid
                WHERE  gtc.gtc_oid = ?
            """.trimIndent(),
            gtcOid,
        ) ?: throw NoRecordFound(tableName, "Could not find a record for gtc_oid = $gtcOid")
        UserHasRun.requireUserRun(connection, userOid, runId)
        connection.executeNoReturn(
            sql = """
                DELETE FROM $tableName
                WHERE  gtc_oid = ?
            """.trimIndent(),
            gtcOid
        )
    }

}
