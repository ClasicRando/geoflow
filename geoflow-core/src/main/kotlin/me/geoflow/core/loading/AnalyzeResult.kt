package me.geoflow.core.loading

import me.geoflow.core.database.composites.Composite
import me.geoflow.core.database.composites.CompositeField
import org.postgresql.util.PGobject
import kotlin.math.max

/** Result container for file analysis. Holds table stats and metadata required for schema */
@Composite("analyze_result")
data class AnalyzeResult(
    /** */
    @CompositeField("st_oid")
    val stOid: Long,
    /** table name for the file/sub table */
    @CompositeField("table_name")
    val tableName: String,
    /** record count for the source table */
    @CompositeField("record_count")
    val recordCount: Int,
    /** column information for the source table */
    val columns: List<ColumnStats>,
): PGobject() {

    init {
        val columnInfo = columns.joinToString(
            separator = "\"\",\"\"",
            prefix = "\"\"",
            postfix = "\"\"",
        ) { it.value ?: "" }
        setValue("($stOid,$tableName,$recordCount,\"{$columnInfo}\")")
    }

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
                    columnType = columnStats.type,
                    index = columnStats.index,
                )
            },
        )
    }

}
