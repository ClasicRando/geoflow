package me.geoflow.core.loading

import me.geoflow.core.database.composites.Composite

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
): Composite("column_info") {

    override val createStatement: String = """
        CREATE TYPE public.column_info AS
        (
        	name text,
        	min_length integer,
        	max_length integer,
        	column_type text,
        	column_index integer
        );
    """.trimIndent()
    override val compositeValue: String = "($name,$minLength,$maxLength,$type,$index)"

}
