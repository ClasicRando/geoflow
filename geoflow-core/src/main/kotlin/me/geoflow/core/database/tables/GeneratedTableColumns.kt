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
object GeneratedTableColumns : DbTable("generated_table_columns"), ApiExposed, Triggers {

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

    @Suppress("MaxLineLength")
    override val triggers: List<Trigger> = listOf(
        Trigger(
            trigger = """
                CREATE TRIGGER trg_check_generation_expression
                    BEFORE INSERT OR UPDATE OF generation_expression
                    ON public.generated_table_columns
                    FOR EACH ROW
                    EXECUTE FUNCTION public.check_generation_expression();
            """.trimIndent(),
            triggerFunction = """
                CREATE OR REPLACE FUNCTION public.check_generation_expression()
                    RETURNS trigger
                    LANGUAGE 'plpgsql'
                    COST 100
                    VOLATILE NOT LEAKPROOF
                AS ${'$'}BODY${'$'}
                DECLARE
                    check_cursor refcursor;
                    source_table_name text;
                    check_buffer text;
                    check_return_values int;
                    expression_check_query text;
                    msg text;
                    error_hint text;
                BEGIN
                    ASSERT NEW.generation_expression != '*', 'generation_expression cannot be "*"';
                    SELECT table_name
                    INTO   source_table_name
                    FROM   source_tables
                    WHERE  st_oid = NEW.st_oid;
                    expression_check_query := 'SELECT '||NEW.generation_expression||' FROM '||source_table_name;
                    BEGIN
                        OPEN check_cursor FOR EXECUTE expression_check_query;
                        FETCH check_cursor INTO check_buffer;
                        CLOSE check_cursor;
                    EXCEPTION
                        WHEN others THEN
                            GET STACKED DIAGNOSTICS
                                msg = message_text,
                                error_hint = pg_exception_hint;
                            RAISE EXCEPTION 'Error while validating generation_expresion "%s". %s', expression_check_query, msg;
                    END;
                    OPEN check_cursor FOR EXECUTE 'SELECT array_length(ARRAY['||COALESCE(NULLIF(NEW.generation_expression,''),'0')||'],1) FROM '||source_table_name;
                    FETCH check_cursor INTO check_return_values;
                    CLOSE check_cursor;
                    ASSERT check_return_values = 1, format('generation_expression cannot return multiple values. Expected 1 got %s', check_return_values);
                    RETURN NEW;
                END;
                ${'$'}BODY${'$'};
            """.trimIndent()
        ),
        Trigger(
            trigger = """
                CREATE TRIGGER trg_check_generation_report_group
                    BEFORE INSERT OR UPDATE OF report_group
                    ON public.generated_table_columns
                    FOR EACH ROW
                    EXECUTE FUNCTION public.check_generation_group();
            """.trimIndent(),
            triggerFunction = """
                CREATE OR REPLACE FUNCTION public.check_generation_group()
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
