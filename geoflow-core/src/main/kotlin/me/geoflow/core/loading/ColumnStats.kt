package me.geoflow.core.loading

/** Container for column metadata and stats. Obtained during file analysis */
data class ColumnStats(
    /** column name */
    val name: String,
    /** column min length of characters */
    val minLength: Int,
    /** column max length of characters */
    val maxLength: Int,
    /** column source data type */
    val type: String = "",
    /** column index within file */
    val index: Int,
)
