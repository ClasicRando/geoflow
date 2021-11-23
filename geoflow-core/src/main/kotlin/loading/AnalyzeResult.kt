package loading

import kotlin.math.max

/** Result container for file analysis. Holds table stats and metadata required for schema */
data class AnalyzeResult(
    /** table name for the file/sub table */
    val tableName: String,
    /** record count for the source table */
    val recordCount: Int,
    /** column information for the source table */
    val columns: List<ColumnStats>,
) {
    /**
     * Merge another [result][analyzeResult] into this result. Adds the 2 record counts and for each column, chooses
     * the higher or lower column lengths for [maxLength][ColumnStats.maxLength] and [minLength][ColumnStats.minLength]
     * respectively. Returns a new [AnalyzeResult] instance without altering any object.
     */
    fun merge(analyzeResult: AnalyzeResult): AnalyzeResult {
        return AnalyzeResult(
            tableName = tableName,
            recordCount = recordCount + analyzeResult.recordCount,
            columns = columns.map { columnStats ->
                val currentStats = analyzeResult.columns.first { columnStats.index == it.index }
                ColumnStats(
                    name = columnStats.name,
                    maxLength = max(columnStats.maxLength, currentStats.maxLength),
                    minLength = Integer.min(columnStats.minLength, currentStats.minLength),
                    index = columnStats.index,
                )
            },
        )
    }
}
