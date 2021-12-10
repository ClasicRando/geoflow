package me.geoflow.core.database.tables.records

import me.geoflow.core.database.extensions.getList
import me.geoflow.core.database.extensions.getListWithNulls
import me.geoflow.core.database.tables.QueryResultRecord
import me.geoflow.core.database.tables.SourceTableColumns
import me.geoflow.core.database.tables.SourceTables
import me.geoflow.core.loading.DEFAULT_DELIMITER
import me.geoflow.core.loading.LoadingInfo
import java.sql.ResultSet

/** Record representing the files required to load */
@QueryResultRecord
class LoadFiles(
    /** name of file to be loaded */
    val fileName: String,
    /** information provided about loading. List of sub table entries */
    val loaders: List<LoadingInfo>,
) {
    @Suppress("UNUSED")
    companion object {
        /** SQL query used to generate the parent class */
        val sql: String = """
                with t1 as (
                    SELECT t1.st_oid,
                           'CREATE table '||t1.table_name||' ('||
                           STRING_AGG(t2.name::text,' text,'::text order by t2.column_index)||
                           ' text)' create_statement
                    FROM   ${SourceTables.tableName} t1
                    JOIN   ${SourceTableColumns.tableName} t2
                    ON     t1.st_oid = t2.st_oid
                    WHERE  t1.run_id = ?
                    AND    t1.load
                    GROUP BY t1.st_oid
                )
                SELECT t2.file_name,
                       array_agg(t1.st_oid order by t1.st_oid) st_oids,
                       array_agg(t2.table_name order by t1.st_oid) table_names,
                       array_agg(t2.sub_table order by t1.st_oid) sub_Tables,
                       array_agg(t2.delimiter order by t1.st_oid) "delimiters",
                       array_agg(t2.qualified order by t1.st_oid) qualified,
                       array_agg(t1.create_statement order by t1.st_oid) create_statements
                FROM   t1
                JOIN   ${SourceTables.tableName} t2
                ON     t1.st_oid = t2.st_oid
                GROUP BY file_name;
            """.trimIndent()
        private const val FILENAME = 1
        private const val ST_OIDS = 2
        private const val TABLE_NAMES = 3
        private const val SUB_TABLES = 4
        private const val DELIMITERS = 5
        private const val QUALIFIED = 6
        private const val CREATE_STATEMENTS = 7
        /** Function used to process a [ResultSet] into a result record */
        fun fromResultSet(rs: ResultSet): LoadFiles {
            val stOids = rs.getArray(ST_OIDS).getList<Long>()
            val tableNames = rs.getArray(TABLE_NAMES).getList<String>()
            val subTables = rs.getArray(SUB_TABLES).getListWithNulls<String>()
            val delimiters = rs.getArray(DELIMITERS).getListWithNulls<String>()
            val areQualified = rs.getArray(QUALIFIED).getList<Boolean>()
            val createStatements = rs.getArray(CREATE_STATEMENTS).getList<String>()
            val info = stOids.mapIndexed { i, stOid ->
                LoadingInfo(
                    stOid,
                    tableName = tableNames[i],
                    createStatement = createStatements[i],
                    delimiter = delimiters[i]?.get(i) ?: DEFAULT_DELIMITER,
                    qualified = areQualified[i],
                    subTable = subTables[i],
                )
            }
            return LoadFiles(fileName = rs.getString(FILENAME), loaders = info)
        }
    }
}
