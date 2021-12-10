package me.geoflow.core.database.tables.records

import me.geoflow.core.database.extensions.getList
import me.geoflow.core.database.extensions.getListWithNulls
import me.geoflow.core.database.tables.QueryResultRecord
import me.geoflow.core.database.tables.SourceTables
import me.geoflow.core.loading.AnalyzeInfo
import me.geoflow.core.loading.DEFAULT_DELIMITER
import java.sql.ResultSet

/** Record representing the files required to analyze */
@QueryResultRecord
class AnalyzeFiles(
    /** name of file to be analyzed */
    val fileName: String,
    /** information provided about analyzing. List of sub table entries */
    val analyzeInfo: List<AnalyzeInfo>,
) {
    @Suppress("UNUSED")
    companion object {
        /** SQL query used to generate the parent class */
        val sql: String = """
                SELECT file_name,
                       array_agg(st_oid order by st_oid) st_oids,
                       array_agg(table_name order by st_oid) table_names,
                       array_agg(sub_table order by st_oid) sub_Tables,
                       array_agg(delimiter order by st_oid) "delimiters",
                       array_agg(qualified order by st_oid) qualified
                FROM   ${SourceTables.tableName}
                WHERE  run_id = ?
                AND    analyze_table
                GROUP BY file_name
            """.trimIndent()
        private const val FILENAME = 1
        private const val ST_OIDS = 2
        private const val TABLE_NAMES = 3
        private const val SUB_TABLES = 4
        private const val DELIMITERS = 5
        private const val QUALIFIED = 6
        /** Function used to process a [ResultSet] into a result record */
        fun fromResultSet(rs: ResultSet): AnalyzeFiles {
            val stOids = rs.getArray(ST_OIDS).getList<Long>()
            val tableNames = rs.getArray(TABLE_NAMES).getList<String>()
            val subTables = rs.getArray(SUB_TABLES).getListWithNulls<String>()
            val delimiters = rs.getArray(DELIMITERS).getListWithNulls<String>()
            val qualified = rs.getArray(QUALIFIED).getList<Boolean>()
            val info = stOids.mapIndexed { i, stOid ->
                AnalyzeInfo(
                    stOid = stOid,
                    tableName = tableNames[i],
                    subTable = subTables[i],
                    delimiter = delimiters[i]?.get(i) ?: DEFAULT_DELIMITER,
                    qualified = qualified[i],
                )
            }
            return AnalyzeFiles(fileName = rs.getString(FILENAME), analyzeInfo = info)
        }
    }
}
