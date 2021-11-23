package loading

/** Container for data used to create and load into tables */
data class LoadingInfo(
    /** unique ID of the source table */
    val stOid: Long,
    /** table name for the file/sub table */
    val tableName: String,
    /** CREATE statement run to make the initial database table */
    val createStatement: String,
    /** delimiter for the source table, if needed */
    val delimiter: Char = DEFAULT_DELIMITER,
    /** flag denoting if the source table data is qualified, if needed */
    val qualified: Boolean = true,
    /** sub table if the source file has multiple tables in a single file, null if the file has a single dataset */
    val subTable: String? = null,
) {
    /** column names extracted from the CREATE statement. Used to set up the COPY command */
    val columns: List<String> = createStatement
        .replace(Regex("^CREATE TABLE [A-Z0-9_]+ \\("), "")
        .replace(Regex(" text\\("), "")
        .replace(" text,", ",")
        .split(",")
        .map { it.trim() }
}
