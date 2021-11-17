package data_loader

data class AnalyzeInfo(
    val stOid: Long,
    val tableName: String,
    val delimiter: Char,
    val qualified: Boolean,
    val subTable: String?,
)