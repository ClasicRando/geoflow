package database.tables

import data_loader.AnalyzeResult
import database.DatabaseConnection
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import orm.enums.FileCollectType
import orm.enums.LoaderType
import orm.tables.ApiExposed
import orm.tables.SequentialPrimaryKey
import orm.tables.SourceTables
import useFirstOrNull
import useMultipleStatements
import java.sql.PreparedStatement
import java.util.*

object SourceTables: DbTable("source_tables"), ApiExposed, SequentialPrimaryKey {

    override val tableDisplayFields = mapOf(
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

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.source_tables
        (
            run_id bigint NOT NULL,
            table_name text COLLATE pg_catalog."default" NOT NULL,
            file_name text COLLATE pg_catalog."default" NOT NULL,
            "analyze" boolean NOT NULL DEFAULT true,
            load boolean NOT NULL DEFAULT true,
            qualified boolean NOT NULL DEFAULT false,
            encoding text COLLATE pg_catalog."default" NOT NULL DEFAULT 'utf8'::text,
            sub_table text COLLATE pg_catalog."default",
            record_count integer NOT NULL DEFAULT 0,
            file_id text COLLATE pg_catalog."default" NOT NULL,
            url text COLLATE pg_catalog."default",
            comments text COLLATE pg_catalog."default",
            st_oid bigint NOT NULL DEFAULT nextval('source_tables_st_oid_seq'::regclass),
            collect_type file_collect_type NOT NULL,
            loader_type loader_type NOT NULL,
            delimiter character varying(1) COLLATE pg_catalog."default",
            CONSTRAINT source_tables_pkey PRIMARY KEY (st_oid),
            CONSTRAINT file_id_run_id_unique UNIQUE (run_id, file_id),
            CONSTRAINT table_name_run_id_unique UNIQUE (run_id, table_name),
            CONSTRAINT table_name_correct CHECK (table_name ~ '^[0-9A-Z_]+${'$'}'::text) NOT VALID
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    /** API response data class for JSON serialization */
    @Serializable
    data class Record(
        @SerialName("st_oid")
        val stOid: Long,
        @SerialName("table_name")
        val tableName: String,
        @SerialName("file_id")
        val fileId: String,
        @SerialName("file_name")
        val fileName: String,
        @SerialName("sub_table")
        val subTable: String?,
        @SerialName("loader_type")
        val loaderType: String,
        @SerialName("delimiter")
        val delimiter: String?,
        @SerialName("qualified")
        val qualified: Boolean,
        @SerialName("encoding")
        val encoding: String,
        @SerialName("url")
        val url: String?,
        @SerialName("comments")
        val comments: String?,
        @SerialName("record_count")
        val recordCount: Int,
        @SerialName("collect_type")
        val collectType: String,
        @SerialName("analyze")
        val analyze: Boolean,
        @SerialName("load")
        val load: Boolean,
    )

    /**
     * Returns JSON serializable response of all source tables linked to a given [runId]. Returns an empty list when
     * no source tables are linked to the [runId]
     */
    suspend fun getRunSourceTables(runId: Long): List<Record> {
        val sql = """
            SELECT st_oid, table_name, file_id, file_name, sub_table, loader_type, delimiter, qualified, encoding, url,
                   comments, record_count, collect_type, $tableName."analyze", load
            FROM   $tableName
            WHERE  run_id = ?
        """.trimIndent()
        return DatabaseConnection.submitQuery(sql, listOf(runId))
    }

    private fun getStatementArguments(map: Map<String, String?>): SortedMap<String, Any?> {
        return sequence<Pair<String, Any?>> {
            for ((key, value) in map.entries) {
                when (key){
                    "table_name" -> {
                        val tableName = value ?: throw IllegalArgumentException("Table name cannot be null")
                        yield(key to tableName)
                    }
                    "file_id" -> {
                        val fileId = value ?: throw IllegalArgumentException("File ID cannot be null")
                        yield(key to fileId)
                    }
                    "file_name" -> {
                        val fileName = value ?: throw IllegalArgumentException("Filename cannot be null")
                        val loaderType = LoaderType.getLoaderType(fileName)
                        if (loaderType == LoaderType.MDB || loaderType == LoaderType.Excel) {
                            val subTable = map["sub_table"] ?: throw IllegalArgumentException(
                                "Sub Table must be not null for the provided filename"
                            )
                            yield("sub_table" to subTable)
                        }
                        yield(key to fileName)
                        yield("loader_type" to loaderType)
                    }
                    "delimiter" -> yield(key to value)
                    "url" -> yield(key to value)
                    "comments" -> yield(key to value)
                    "collect_type" -> {
                        yield(key to FileCollectType.valueOf(value ?: "").pgObject)
                    }
                    "qualified" -> yield(key to value.equals("on"))
                    "analyze" -> yield(key to value.equals("on"))
                    "load" -> yield(key to value.equals("on"))
                }
            }
        }.toMap().toSortedMap()
    }

    private fun PreparedStatement.setParameters(map: SortedMap<String, Any?>) {
        map.entries.forEachIndexed { index, (_, value) ->
            setObject(index, value)
        }
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
    suspend fun updateSourceTable(username: String, params: Map<String, String?>): Pair<Long, Int> {
        val runId = params["runId"]
            ?.toLong()
            ?: throw IllegalArgumentException("runId must be a non-null parameter in the url")
        val stOid = params["stOid"]
            ?.toLong()
            ?: throw IllegalArgumentException("stOid must be a non-null parameter in the url")
        require(PipelineRuns.checkUserRun(runId, username)) { "Username does not own the runId" }
        val sortedMap = getStatementArguments(params)
        return DatabaseConnection.queryConnectionSingle { connection ->
            connection.prepareStatement("""
                UPDATE $tableName
                SET    ${sortedMap.keys.joinToString { key -> "$key = ?" }}
                WHERE  st_oid = ?
            """.trimIndent())
                .apply { setParameters(sortedMap) }
                .use { statement -> stOid to statement.executeUpdate() }
        }
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
    suspend fun insertSourceTable(username: String, params: Map<String, String?>): Long {
        val runId = params["runId"]
            ?.toLong()
            ?: throw IllegalArgumentException("runId must be a non-null parameter in the url")
        require(PipelineRuns.checkUserRun(runId, username)) { "Username does not own the runId" }
        val sortedMap = getStatementArguments(params)
        return DatabaseConnection.queryConnectionSingle { connection ->
            connection.prepareStatement("""
                INSERT INTO $tableName (${sortedMap.keys.joinToString()})
                VALUES (${"?,".repeat(sortedMap.size).trim(',')})
                RETURNING st_oid
            """.trimIndent())
                .apply { setParameters(sortedMap) }
                .use { statement ->
                    statement.execute()
                    statement.resultSet.useFirstOrNull { rs ->
                        rs.getLong(1)
                    } ?: throw IllegalArgumentException("Error while trying to insert record. Null returned")
                }
        }
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
    suspend fun deleteSourceTable(username: String, params: Map<String, String?>): Long {
        val runId = params["runId"]
            ?.toLong()
            ?: throw IllegalArgumentException("runId must be a non-null parameter in the url")
        val stOid = params["stOid"]
            ?.toLong()
            ?: throw IllegalArgumentException("stOid must be a non-null parameter in the url")
        require(PipelineRuns.checkUserRun(runId, username)) { "Username does not own the runId" }
        DatabaseConnection.execute { connection ->
            connection.prepareStatement("DELETE FROM $tableName WHERE st_oid = ?").apply {
                setLong(1, stOid)
            }.use { statement ->
                statement.execute()
            }
        }
        return stOid
    }

    data class AnalyzeFiles(
        val fileName: String,
        val stOids: List<Long>,
        val tableNames: List<String>,
        val subTables: List<String>,
        val delimiter: String?,
        val qualified: Boolean,
    )

    suspend fun filesToAnalyze(runId: Long): List<AnalyzeFiles> {
        return DatabaseConnection.queryConnection { connection ->
            connection.prepareStatement("""
                with t1 as (
                    SELECT file_name,
                           array_agg(st_oid order by st_oid) st_oids,
                           array_agg(table_name order by st_oid) table_names,
                           array_agg(sub_table order by st_oid) sub_Tables,
                           array_agg(delimiter order by st_oid) "delimiter",
                           array_agg(qualified order by st_oid) qualified
                    FROM   $tableName
                    WHERE  run_id = ?
                    AND    "analyze"
                    GROUP BY file_name
                )
                select file_name, st_oids, table_names, sub_Tables, delimiter[1] "delimiter", qualified[1] qualified
                from   t1
            """.trimIndent()).apply {
                setLong(1, runId)
            }.use { statement ->
                statement.executeQuery().use { rs ->
                    generateSequence {
                        if (rs.next()) {
                            AnalyzeFiles(
                                rs.getString(1),
                                (rs.getArray(2).array as Array<*>).mapNotNull {
                                    if (it is Long) it else null
                                },
                                (rs.getArray(3).array as Array<*>).mapNotNull {
                                    if (it is String) it else null
                                },
                                (rs.getArray(4).array as Array<*>).mapNotNull {
                                    if (it is String) it else null
                                },
                                rs.getString(5),
                                rs.getBoolean(6),
                            )
                        } else {
                            null
                        }
                    }.toList()
                }
            }
        }
    }

    suspend fun finishAnalyze(data: Map<Long, AnalyzeResult>) {
        DatabaseConnection.execute { connection ->
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
                SET    $tableName."analyze" = false,
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
                            "${column.name}_${repeatCount}"
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
    }
}