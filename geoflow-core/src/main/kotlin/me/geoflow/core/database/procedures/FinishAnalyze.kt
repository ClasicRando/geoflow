package me.geoflow.core.database.procedures

import me.geoflow.core.loading.AnalyzeResult
import kotlin.reflect.typeOf

/** */
@Suppress("unused")
object FinishAnalyze : SqlProcedure(
    name = "finish_analyze",
    parameterTypes = listOf(
        typeOf<Array<AnalyzeResult>>()
    )
) {
    @Suppress("MaxLineLength")
    override val code: String = """
        CREATE OR REPLACE PROCEDURE public.finish_analyze(
        	results analyze_result[])
        LANGUAGE 'sql'
        AS ${'$'}BODY${'$'}
        WITH results AS (
        	SELECT (t.r).st_oid, (t.r).table_name, (t.r).record_count, unnest((t.r).columns) column_info
        	FROM  (SELECT unnest($1) r) t
        ), dup_check AS (
        	SELECT r.st_oid, (r.column_info).*,
        		   COUNT((r.column_info).name) OVER (PARTITION BY st_oid, (r.column_info).name) name_repeat_count,
        		   RANK() OVER (PARTITION BY r.st_oid, (r.column_info).name order BY (r.column_info).column_index) name_repeat_rank
        	FROM   results r
        ), table_group_number AS (
            SELECT st.st_oid, ROW_NUMBER() OVER (ORDER BY st.st_oid) report_group
            FROM   source_tables st
            JOIN  (SELECT distinct st.run_id
                   FROM   results r
                   JOIN   source_tables st
                   ON     r.st_oid = st.st_oid) t
            ON     st.run_id = t.run_id
        )
        INSERT INTO source_table_columns(st_oid,name,type,max_length,min_length,label,column_index,report_group)
        SELECT d.st_oid, CASE WHEN d.name_repeat_count > 1 THEN d.name||'_'||d.name_repeat_rank ELSE d.name END,
        	   d.column_type, d.max_length, d.min_length, d.name, d.column_index, t.report_group
        FROM   dup_check d
        JOIN   table_group_number t
        ON     d.st_oid = t.st_oid
        ON CONFLICT (st_oid, name)
        DO UPDATE SET type = EXCLUDED.type,
                      max_length = EXCLUDED.max_length,
                      min_length = EXCLUDED.min_length,
                      column_index = EXCLUDED.column_index;

        WITH results AS (
        	SELECT DISTINCT (t.r).st_oid, (t.r).record_count
        	FROM  (SELECT unnest($1) r) t
        )
        UPDATE source_tables st
        SET    analyze_table = FALSE,
               record_count = r.record_count
        FROM   results r
        WHERE  st.st_oid = r.st_oid;
        ${'$'}BODY${'$'};
    """.trimIndent()
}
