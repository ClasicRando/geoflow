package me.geoflow.core.loading

import me.geoflow.core.database.composites.Composite
import kotlin.math.max

/** Result container for file analysis. Holds table stats and metadata required for schema */
data class AnalyzeResult(
    /** */
    val stOid: Long,
    /** table name for the file/sub table */
    val tableName: String,
    /** record count for the source table */
    val recordCount: Int,
    /** column information for the source table */
    val columns: List<ColumnStats>,
): Composite("analyze_result") {
    /**
     * Merge another [result][analyzeResult] into this result. Adds the 2 record counts and for each column, chooses
     * the higher or lower column lengths for [maxLength][ColumnStats.maxLength] and [minLength][ColumnStats.minLength]
     * respectively. Returns a new [AnalyzeResult] instance without altering any object.
     */
    fun merge(analyzeResult: AnalyzeResult): AnalyzeResult {
        return AnalyzeResult(
            stOid = stOid,
            tableName = tableName,
            recordCount = recordCount + analyzeResult.recordCount,
            columns = columns.map { columnStats ->
                val currentStats = analyzeResult.columns.first { columnStats.index == it.index }
                ColumnStats(
                    name = columnStats.name,
                    maxLength = max(columnStats.maxLength, currentStats.maxLength),
                    minLength = Integer.min(columnStats.minLength, currentStats.minLength),
                    type = columnStats.type,
                    index = columnStats.index,
                )
            },
        )
    }

    override val createStatement: String = """
        CREATE TYPE public.analyze_result AS
        (
        	st_oid bigint,
        	table_name text,
        	record_count integer,
        	columns column_info[]
        );
    """.trimIndent()
    override val compositeValue: String get() {
        val columnInfo = columns.joinToString(
            separator = "\"\",\"\"",
            prefix = "\"\"",
            postfix = "\"\"",
        ) { it.compositeValue }
        return "($stOid,$tableName,$recordCount,\"{$columnInfo}\")"
    }

}
