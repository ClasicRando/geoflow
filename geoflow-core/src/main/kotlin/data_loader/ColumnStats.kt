package data_loader

/** Container for column metadata and stats. Obtained during file analysis */
data class ColumnStats(val name: String, val minLength: Int, val maxLength: Int, val type: String = "", val index: Int)