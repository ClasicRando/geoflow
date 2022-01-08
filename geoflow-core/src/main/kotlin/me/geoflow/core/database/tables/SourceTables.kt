package me.geoflow.core.database.tables

import me.geoflow.core.database.errors.NoRecordAffected
import me.geoflow.core.database.errors.NoRecordFound
import me.geoflow.core.database.extensions.queryFirstOrNull
import me.geoflow.core.loading.AnalyzeResult
import me.geoflow.core.database.extensions.runReturningFirstOrNull
import me.geoflow.core.database.extensions.useMultipleStatements
import java.sql.Connection
import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.functions.UserHasRun
import me.geoflow.core.database.tables.records.AnalyzeFiles
import me.geoflow.core.database.tables.records.LoadFiles
import me.geoflow.core.database.tables.records.SourceTable
import me.geoflow.core.database.tables.records.TableCountComparison

/**
 * Table used to store the source table and file information used for all pipeline runs. Parent to [SourceTableColumns]
 */
object SourceTables : DbTable("source_tables"), ApiExposed {

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "table_name" to mapOf("editable" to "true", "sortable" to "true"),
        "file_id" to mapOf("title" to "File ID", "editable" to "true", "sortable" to "true"),
        "file_name" to mapOf("editable" to "true", "sortable" to "true"),
        "sub_table" to mapOf("editable" to "true"),
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
            analyze_table boolean NOT NULL DEFAULT true,
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

    /**
     * Returns a JSON serializable response of a single record specified by the [stOid]
     */
    fun getRunId(connection: Connection, stOid: Long): Long {
        return connection.queryFirstOrNull(sql = "SELECT run_id FROM $tableName WHERE st_oid = ?", stOid)
            ?: throw NoRecordFound(tableName, "Could not find a record for st_oid = $stOid")
    }

    /**
     * Returns JSON serializable response of all source tables linked to a given [runId]. Returns an empty list when
     * no source tables are linked to the [runId]
     */
    fun getRunSourceTables(connection: Connection, runId: Long): List<SourceTable> {
        val sql = """
            SELECT st_oid, table_name, file_id, file_name, sub_table, delimiter, qualified, encoding, url,
                   comments, record_count, collect_type, analyze_table, load
            FROM   $tableName
            WHERE  run_id = ?
        """.trimIndent()
        return connection.submitQuery(sql = sql, runId)
    }

    /**
     * Updates a source table record with the details from the [requestBody] object. Must be run in transaction to lock
     * selected record for update.
     *
     * @throws java.sql.SQLException when the connection throws an exception
     * @throws NoRecordAffected when:
     * - the update does not affect any records
     * - the user does not have the ability to operate on this run
     */
    fun updateSourceTable(
        connection: Connection,
        userOid: Long,
        requestBody: SourceTable,
    ): SourceTable {
        val sql = """
            UPDATE $tableName
            SET    table_name = ?,
                   file_id = ?,
                   file_name = ?,
                   sub_table = ?,
                   loader_type = ?,
                   delimiter = ?,
                   qualified = ?,
                   encoding = ?,
                   url = ?,
                   comments = ?,
                   collect_type = ?,
                   analyze_table = ?,
                   load = ?
            WHERE  user_has_run(?,run_id)
            AND    st_oid = ?
            RETURNING st_oid,table_name,file_id,file_name,sub_table,loader_type,delimiter,qualified,encoding,url,
            comments,record_count,collect_type,analyze_table,load
        """.trimIndent()
        return connection.runReturningFirstOrNull(
            sql = sql,
            requestBody.tableName,
            requestBody.fileId,
            requestBody.fileName,
            requestBody.subTable.takeIf { it?.isNotBlank() ?: false },
            requestBody.loaderType.pgObject,
            requestBody.delimiter.takeIf { it?.isNotBlank() ?: false },
            requestBody.qualified,
            requestBody.encoding,
            requestBody.url,
            requestBody.comments.takeIf { it?.isNotBlank() ?: false },
            requestBody.fileCollectType.pgObject,
            requestBody.analyze,
            requestBody.load,
            userOid,
            requestBody.stOid,
        ) ?: throw NoRecordAffected(tableName, "Update did not return any records")
    }

    /**
     * Creates a new source table record with the details from the [requestBody] object.
     *
     * @throws java.sql.SQLException when the connection throws an exception
     * @throws IllegalArgumentException when the username provided does not have the ability to update this run
     * @throws NoRecordAffected when the insert does not create/affect any records
     */
    fun insertSourceTable(
        connection: Connection,
        runId: Long,
        userOid: Long,
        requestBody: SourceTable,
    ): Long {
        require(UserHasRun.checkUserRun(connection, userOid, runId)) { "Username does not own the runId" }
        val sql = """
            INSERT INTO $tableName (run_id,table_name,file_id,file_name,sub_table,loader_type,delimiter,qualified,
                                    encoding,url,comments,collect_type,analyze_table,load)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            RETURNING st_oid
        """.trimIndent()
        return connection.runReturningFirstOrNull(
            sql = sql,
            runId,
            requestBody.tableName,
            requestBody.fileId,
            requestBody.fileName,
            requestBody.subTable.takeIf { it?.isNotBlank() ?: false },
            requestBody.loaderType.pgObject,
            requestBody.delimiter.takeIf { it?.isNotBlank() ?: false },
            requestBody.qualified,
            requestBody.encoding,
            requestBody.url,
            requestBody.comments.takeIf { it?.isNotBlank() ?: false },
            requestBody.fileCollectType.pgObject,
            requestBody.analyze,
            requestBody.load,
        ) ?: throw NoRecordAffected(tableName, message = "Error while trying to insert record. Nothing returned")
    }

    /**
     * Uses [stOid] to delete a record from [SourceTables]
     *
     * @throws java.sql.SQLException when the connection throws an exception
     * @throws NoRecordAffected:
     * - delete command does not affect any records
     * - the username provided does not have the ability to update this run
     */
    fun deleteSourceTable(connection: Connection, stOid: Long, userOid: Long) {
        connection.runReturningFirstOrNull<Long>(
            sql = "DELETE FROM $tableName WHERE st_oid = ? AND user_has_run(?,run_id) RETURNING st_oid",
            stOid,
            userOid,
        ) ?: throw NoRecordAffected(tableName, "No record deleted for st_oid = $stOid")
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
            VALUES(?,?,?,?,?,?,?)
            ON CONFLICT (st_oid, name) DO UPDATE SET type = ?,
                                                     max_length = ?,
                                                     min_length = ?,
                                                     column_index = ?,
                                                     label = ?
        """.trimIndent()
        val tableSql = """
            UPDATE $tableName
            SET    analyze_table = false,
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
                    columnStatement.setString(6, columnName)
                    columnStatement.setInt(7, column.index)
                    columnStatement.setString(8, column.type)
                    columnStatement.setInt(9, column.maxLength)
                    columnStatement.setInt(10, column.minLength)
                    columnStatement.setInt(11, column.index)
                    columnStatement.setString(12, columnName)
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

    /** Returns a list of files to load using the [runId] provided */
    fun filesToLoad(connection: Connection, runId: Long): List<LoadFiles> {
        return connection.submitQuery(sql = LoadFiles.sql, runId)
    }

    /** */
    val tableCountComparisonFields: Map<String, Map<String, String>> = mapOf(
        "current_table_name" to mapOf(),
        "current_file_name" to mapOf(),
        "current_record_count" to mapOf(),
        "last_table_name" to mapOf(),
        "last_file_name" to mapOf(),
        "last_record_count" to mapOf(),
    )

    /** */
    fun tableCountComparison(connection: Connection, runId: Long): List<TableCountComparison> {
        val lastRun = PipelineRuns.lastRun(connection, runId) ?: throw IllegalArgumentException(
            "The data source linked to run_id = $runId must have a previous run to compare"
        )
        return connection.submitQuery(
            sql = """
                SELECT t1.st_oid, t1.table_name, t1.file_name, t1.record_count,
                       t2.table_name, t2.file_name, t2.record_count
                FROM  (SELECT * FROM $tableName WHERE run_id = ?) t1
                JOIN  (SELECT * FROM $tableName WHERE run_id = ?) t2
                ON     t1.file_id = t2.file_id
            """.trimIndent(),
            runId,
            lastRun,
        )
    }

}
