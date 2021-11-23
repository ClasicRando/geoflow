package database.tables

import loading.AnalyzeInfo
import loading.AnalyzeResult
import loading.LoadingInfo
import loading.DEFAULT_DELIMITER
import database.enums.FileCollectType
import database.enums.LoaderType
import database.extensions.executeNoReturn
import database.extensions.runReturningFirstOrNull
import database.extensions.runUpdate
import database.extensions.getListWithNulls
import database.extensions.getList
import database.extensions.useMultipleStatements
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.util.SortedMap
import database.extensions.submitQuery
import java.sql.ResultSet

/**
 * Table used to store the source table and file information used for all pipeline runs. Parent to [SourceTableColumns]
 */
object SourceTables : DbTable("source_tables"), ApiExposed {

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "table_name" to mapOf("editable" to "true", "sortable" to "true"),
        "file_id" to mapOf("name" to "File ID", "editable" to "true", "sortable" to "true"),
        "file_name" to mapOf("editable" to "true", "sortable" to "true"),
        "sub_table" to mapOf("editable" to "true"),
        "loader_type" to mapOf("editable" to "false"),
        "delimiter" to mapOf("editable" to "true"),
        "qualified" to mapOf("editable" to "true", "formatter" to "boolFormatter"),
        "encoding" to mapOf("editable" to "false"),
        "url" to mapOf("editable" to "true"),
        "comments" to mapOf("editable" to "true"),
        "record_count" to mapOf("editable" to "false"),
        "collect_type" to mapOf("editable" to "true"),
        "analyze" to mapOf("editable" to "true", "formatter" to "boolFormatter"),
        "load" to mapOf("editable" to "true", "formatter" to "boolFormatter"),
        "action" to mapOf("formatter" to "actionFormatter"),
    )

    @Suppress("MaxLineLength")
    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.source_tables
        (
            st_oid bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            run_id bigint NOT NULL REFERENCES public.pipeline_runs (run_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
            table_name text COLLATE pg_catalog."default" NOT NULL
				CHECK (table_name ~ '^[0-9A-Z_]+$'::text),
            file_name text COLLATE pg_catalog."default" NOT NULL CHECK (file_name ~ '^.+\..+$'),
            "analyze" boolean NOT NULL DEFAULT true,
            load boolean NOT NULL DEFAULT true,
            qualified boolean NOT NULL DEFAULT false,
            encoding text COLLATE pg_catalog."default" NOT NULL DEFAULT 'utf8'::text CHECK (check_not_blank_or_empty(encoding)),
            sub_table text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(sub_table)),
            record_count integer NOT NULL DEFAULT 0,
            file_id text COLLATE pg_catalog."default" NOT NULL CHECK (file_id ~ '^F\d+$'),
            url text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(url)),
            comments text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(comments)),
            collect_type file_collect_type NOT NULL,
            loader_type loader_type NOT NULL,
            delimiter character varying(1) COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(delimiter::text)),
            CONSTRAINT unique_file_id_run_id UNIQUE (run_id, file_id),
            CONSTRAINT unique_table_name_run_id UNIQUE (run_id, table_name)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    /** API response data class for JSON serialization */
    @Serializable
    data class Record(
        /** unique ID of the pipeline run */
        @SerialName("st_oid")
        val stOid: Long,
        /** database table name the file (and sub table if needed) */
        @SerialName("table_name")
        val tableName: String,
        /** filed id of the source table. Used as a runId specific code */
        @SerialName("file_id")
        val fileId: String,
        /** source filename for the source table */
        @SerialName("file_name")
        val fileName: String,
        /** if the file has sub tables (ie mdb or excel), the name if used to collect the right data */
        @SerialName("sub_table")
        val subTable: String?,
        /** classification of the loader type for the file. Name of the enum value */
        @SerialName("loader_type")
        val loaderType: String,
        /** delimiter of the data, if required */
        @SerialName("delimiter")
        val delimiter: String?,
        /** flag denoting if the data is qualified, if required */
        @SerialName("qualified")
        val qualified: Boolean,
        /** encoding of the file */
        @SerialName("encoding")
        val encoding: String,
        /** url to obtain the data */
        @SerialName("url")
        val url: String?,
        /** comments about the source table */
        @SerialName("comments")
        val comments: String?,
        /** scanned record count of the source table */
        @SerialName("record_count")
        val recordCount: Int,
        /** collection method to obtain the file. Name of the enum value */
        @SerialName("collect_type")
        val collectType: String,
        /** flag denoting if the source table has been analyzed */
        @SerialName("analyze")
        val analyze: Boolean,
        /** flag denoting if the source table has been loaded */
        @SerialName("load")
        val load: Boolean,
    )

    /**
     * Returns JSON serializable response of all source tables linked to a given [runId]. Returns an empty list when
     * no source tables are linked to the [runId]
     */
    fun getRunSourceTables(connection: Connection, runId: Long): List<Record> {
        val sql = """
            SELECT st_oid, table_name, file_id, file_name, sub_table, loader_type, delimiter, qualified, encoding, url,
                   comments, record_count, collect_type, $tableName."analyze", load
            FROM   $tableName
            WHERE  run_id = ?
        """.trimIndent()
        return connection.submitQuery(sql = sql, runId)
    }

    /**
     * Builds a [SortedMap] to provide ordered key value pairs for record updating/inserting
     */
    @Suppress("ComplexMethod")
    private fun getStatementArguments(map: Map<String, String>): SortedMap<String, Any?> {
        return buildMap {
            for ((key, value) in map.entries) {
                when (key){
                    "table_name" -> {
                        set(key, value)
                    }
                    "file_id" -> {
                        set(key, value)
                    }
                    "file_name" -> {
                        val loaderType = LoaderType.getLoaderType(value)
                        if (loaderType == LoaderType.MDB || loaderType == LoaderType.Excel) {
                            val subTable = map["sub_table"] ?: throw IllegalArgumentException(
                                "Sub Table must be not null for the provided filename"
                            )
                            set("sub_table", subTable)
                        }
                        set(key, value)
                        set("loader_type", loaderType.pgObject)
                    }
                    "delimiter" -> set(key, value.takeIf { it.isNotBlank() })
                    "url" -> set(key, value.takeIf { it.isNotBlank() })
                    "comments" -> set(key, value.takeIf { it.isNotBlank() })
                    "collect_type" -> {
                        set(key, FileCollectType.valueOf(value).pgObject)
                    }
                    "qualified" -> set(key, value == "on")
                    "analyze" -> set(key, value == "on")
                    "load" -> set(key, value == "on")
                }
            }
        }.toSortedMap()
    }

    /**
     * Uses [params] map to update a given record specified by the stOid provided in the map and return the stOid
     *
     * @throws IllegalArgumentException when various conditions are not met
     * - [params] does not contain runId
     * - [params] does not contain stOid
     * - the username passed does not have access to update the source tables associated with the runId
     * @throws NumberFormatException when the runId or stOid are not Long strings
     */
    fun updateSourceTable(
        connection: Connection,
        username: String,
        params: Map<String, String>
    ): Pair<Long, Int> {
        val runId = params["runId"]
            ?.toLong()
            ?: throw IllegalArgumentException("runId must be a non-null parameter in the url")
        val stOid = params["stOid"]
            ?.toLong()
            ?: throw IllegalArgumentException("stOid must be a non-null parameter in the url")
        require(PipelineRuns.checkUserRun(connection, runId, username)) { "Username does not own the runId" }
        val sortedMap = getStatementArguments(params)
        val sql = """
            UPDATE $tableName
            SET    ${sortedMap.keys.joinToString { key -> "$key = ?" }}
            WHERE  st_oid = ?
        """.trimIndent()
        val updateCount = connection.runUpdate(
            sql = sql,
            sortedMap.values,
            stOid,
        )
        return stOid to updateCount
    }

    /**
     * Uses [params] map to insert a record into [SourceTables] and return the new records stOid
     *
     * @throws IllegalArgumentException when various conditions are not met
     * - [params] does not contain runId
     * - the username passed does not have access to update the source tables associated with the runId
     * - the insert command returns null meaning a record was not inserted
     * @throws NumberFormatException when the runId or stOid are not Long strings
     */
    fun insertSourceTable(
        connection: Connection,
        username: String,
        params: Map<String, String>,
    ): Long {
        val runId = params["runId"]
            ?.toLong()
            ?: throw IllegalArgumentException("runId must be a non-null parameter in the url")
        require(PipelineRuns.checkUserRun(connection, runId, username)) { "Username does not own the runId" }
        val sortedMap = getStatementArguments(params)
        val sql = """
            INSERT INTO $tableName (run_id,${sortedMap.keys.joinToString()})
            VALUES (?,${"?,".repeat(sortedMap.size).trim(',')})
            RETURNING st_oid
        """.trimIndent()
        return connection.runReturningFirstOrNull(
            sql = sql,
            runId,
            sortedMap.values,
        ) ?: throw IllegalArgumentException("Error while trying to insert record. Null returned")
    }

    /**
     * Uses [params] map to delete a record from [SourceTables] as specified by the stOid and return the stOid
     *
     * @throws IllegalArgumentException when various conditions are not met
     * - [params] does not contain runId
     * - [params] does not contain stOid
     * - the username passed does not have access to update the source tables associated with the runId
     * @throws NumberFormatException when the runId or stOid are not Long strings
     */
    fun deleteSourceTable(connection: Connection, username: String, params: Map<String, String?>): Long {
        val runId = params["runId"]
            ?.toLong()
            ?: throw IllegalArgumentException("runId must be a non-null parameter in the url")
        val stOid = params["stOid"]
            ?.toLong()
            ?: throw IllegalArgumentException("stOid must be a non-null parameter in the url")
        require(PipelineRuns.checkUserRun(connection, runId, username)) { "Username does not own the runId" }
        connection.executeNoReturn(sql = "DELETE FROM $tableName WHERE st_oid = ?", stOid)
        return stOid
    }

    /** Record representing the files required to analyze */
    @QueryResultRecord
    data class AnalyzeFiles(
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
                FROM   $tableName
                WHERE  run_id = ?
                AND    "analyze"
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

    /** Returns a list of files to analyze using the [runId] provided */
    fun filesToAnalyze(connection: Connection, runId: Long): List<AnalyzeFiles> {
        return connection.submitQuery(sql = AnalyzeFiles.sql, runId)
    }

    /**
     * Finalizes the analysis process by inserting or updating [SourceTableColumns] records for all the source tables
     * as well as updating all source tables to *analyze = false*
     */
    @Suppress("MagicNumber")
    fun finishAnalyze(connection: Connection, data: Map<Long, AnalyzeResult>) {
        val columnSql = """
            INSERT INTO ${SourceTableColumns.tableName}(st_oid,name,type,max_length,min_length,label,column_index)
            VALUES(?,?,?,?,?,'',?)
            ON CONFLICT (st_oid, name) DO UPDATE SET type = ?,
                                                     max_length = ?,
                                                     min_length = ?,
                                                     column_index = ?
        """.trimIndent()
        val tableSql = """
            UPDATE $tableName
            SET    "analyze" = false,
                   record_count = ?
            WHERE  st_oid = ?
        """.trimIndent()
        connection.useMultipleStatements(listOf(columnSql, tableSql)) { statements ->
            val columnStatement = statements.getOrNull(0)
                ?: throw IllegalStateException("Column statement must exist")
            val tableStatement = statements.getOrNull(1)
                ?: throw IllegalStateException("Table statement must exist")
            for ((stOid, analyzeResult) in data) {
                val repeats = analyzeResult.columns
                    .groupingBy { it.name }
                    .eachCount()
                    .filter { it.value > 1 }
                    .toMutableMap()
                for (column in analyzeResult.columns) {
                    val columnName = repeats[column.name]?.let { repeatCount ->
                        repeats[column.name] = repeatCount - 1
                        "${column.name}_$repeatCount"
                    } ?: column.name
                    columnStatement.setLong(1, stOid)
                    columnStatement.setString(2, columnName)
                    columnStatement.setString(3, column.type)
                    columnStatement.setInt(4, column.maxLength)
                    columnStatement.setInt(5, column.minLength)
                    columnStatement.setInt(6, column.index)
                    columnStatement.setString(7, column.type)
                    columnStatement.setInt(8, column.maxLength)
                    columnStatement.setInt(9, column.minLength)
                    columnStatement.setInt(10, column.index)
                    columnStatement.addBatch()
                }
                tableStatement.setInt(1, analyzeResult.recordCount)
                tableStatement.setLong(2, stOid)
                tableStatement.addBatch()
            }
            columnStatement.executeBatch()
            tableStatement.executeBatch()
        }
    }

    /** Record representing the files required to load */
    @QueryResultRecord
    data class LoadFiles(
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
                    FROM   $tableName t1
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
                JOIN   $tableName t2
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

    /** Returns a list of files to load using the [runId] provided */
    fun filesToLoad(connection: Connection, runId: Long): List<LoadFiles> {
        return connection.submitQuery(sql = LoadFiles.sql, runId)
    }
}
