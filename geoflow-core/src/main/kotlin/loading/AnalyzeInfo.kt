package loading

/** Container for data used to analyze source tables */
data class AnalyzeInfo(
    /** unique ID of the source table */
    val stOid: Long,
    /** table name for the file/sub table */
    val tableName: String,
    /** delimiter for the source table, if needed */
    val delimiter: Char = defaultDelimiter,
    /** flag denoting if the source table data is qualified, if needed */
    val qualified: Boolean = true,
    /** sub table if the source file has multiple tables in a single file, null if the file has a single dataset */
    val subTable: String? = null,
)
