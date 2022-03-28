package me.geoflow.core.loading

import me.geoflow.core.database.composites.Composite
import me.geoflow.core.database.composites.CompositeField
import org.postgresql.util.PGobject

/** Container for column metadata and stats. Obtained during file analysis */
@Composite("column_info")
data class ColumnStats(
    /** column name */
    val name: String,
    /** column min length of characters */
    val minLength: Int,
    /** column max length of characters */
    val maxLength: Int,
    /** column source data type */
    @CompositeField("type")
    val columnType: String = "",
    /** column index within file */
    val index: Int,
): PGobject() {

    init {
        setValue("($name,$minLength,$maxLength,$columnType,$index)")
    }

}
