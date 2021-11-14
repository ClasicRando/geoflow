package data_loader

/** Container for data used to create and load into tables */
data class LoadingInfo(
    val stOid: Long,
    val tableName: String,
    val createStatement: String,
    val delimiter: Char = defaultDelimiter,
    val qualified: Boolean = true,
    val subTable: String? = null,
) {
    val columns = createStatement
        .replace(Regex("^CREATE TABLE [A-Z0-9_]+ \\("), "")
        .replace(Regex(" text\\("), "")
        .replace(" text,", ",")
        .split(",")
        .map { it.trim() }
}