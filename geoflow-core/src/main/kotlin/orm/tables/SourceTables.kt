package orm.tables

import database.DatabaseConnection
import database.sourceTables
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.ktorm.dsl.AssignmentsBuilder
import org.ktorm.dsl.delete
import org.ktorm.dsl.eq
import org.ktorm.dsl.update
import org.ktorm.entity.filter
import org.ktorm.entity.map
import org.ktorm.schema.*
import org.ktorm.support.postgresql.insertReturning
import orm.entities.SourceTable
import orm.enums.FileCollectType
import orm.enums.LoaderType
import kotlin.jvm.Throws

object SourceTables: Table<SourceTable>("source_tables") {

    val stOid = long("st_oid").primaryKey().bindTo { it.stOid }
    val runId = long("run_id").bindTo { it.runId }
    val sourceTableName = text("table_name").bindTo { it.tableName }
    val fileName = text("file_name").bindTo { it.fileName }
    val analyze = boolean("analyze").bindTo { it.analyze }
    val load = boolean("load").bindTo { it.load }
    val loaderType = enum<LoaderType>("loader_type").bindTo { it.loaderType }
    val qualified = boolean("qualified").bindTo { it.qualified }
    val encoding = text("encoding").bindTo { it.encoding }
    val subTable = text("sub_table").bindTo { it.subTable }
    val recordCount = int("record_count").bindTo { it.recordCount }
    val fileId = text("file_id").bindTo { it.fileId }
    val url = text("url").bindTo { it.url }
    val comments = text("comments").bindTo { it.comments }
    val collectType = enum<FileCollectType>("collect_type").bindTo { it.collectType }
    val delimiter = varchar("delimiter").bindTo { it.delimiter }
    val tableDisplayFields = mapOf(
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
    )

    val createSequence = """
        CREATE SEQUENCE public.source_tables_st_oid_seq
            INCREMENT 1
            START 1
            MINVALUE 1
            MAXVALUE 9223372036854775807
            CACHE 1;
    """.trimIndent()
    val createStatement = """
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
        val subTable: String,
        @SerialName("loader_type")
        val loaderType: String,
        @SerialName("delimiter")
        val delimiter: String,
        @SerialName("qualified")
        val qualified: Boolean,
        @SerialName("encoding")
        val encoding: String,
        @SerialName("url")
        val url: String,
        @SerialName("comments")
        val comments: String,
        @SerialName("record_count")
        val recordCount: Int,
        @SerialName("collect_type")
        val collectType: String,
        @SerialName("analyze")
        val analyze: Boolean,
        @SerialName("load")
        val load: Boolean,
    )

    @Throws(IllegalArgumentException::class)
    fun getRunSourceTables(runId: Long): List<Record> {
        return DatabaseConnection
            .database
            .sourceTables
            .filter { it.runId eq runId }
            .map { table ->
                Record(
                    table.stOid,
                    table.tableName,
                    table.fileId,
                    table.fileName,
                    table.subTable ?: "",
                    table.loaderType.name,
                    table.delimiter ?: "",
                    table.qualified,
                    table.encoding,
                    table.url ?: "",
                    table.comments ?: "",
                    table.recordCount,
                    table.collectType.name,
                    table.analyze,
                    table.load,
                )
            }
    }

    private fun AssignmentsBuilder.buildAssignmentFromParams(params: Map<String, String?>) {
        if ("table_name" in params) {
            set(
                sourceTableName,
                params["table_name"] ?: throw IllegalArgumentException("Table name cannot be null")
            )
        }
        if ("file_id" in params) {
            set(fileId, params["file_id"] ?: throw IllegalArgumentException("File ID cannot be null"))
        }
        if ("file_name" in params) {
            val fileName = params["file_name"] ?: throw IllegalArgumentException("Filename cannot be null")
            val fileExtension = "(?<=\\.)[^.]+\$".toRegex().find(fileName)?.value
                ?: throw IllegalArgumentException("file extension could not be found")
            val loaderType = LoaderType.values().first { fileExtension in it.extensions }
            if (loaderType == LoaderType.MDB || loaderType == LoaderType.Excel) {
                val subTable = params["sub_table"]
                    ?: throw IllegalArgumentException("Sub Table must be not null for the provided filename")
                set(SourceTables.subTable, subTable)
            }
            set(SourceTables.fileName, fileName)
            set(SourceTables.loaderType, loaderType)
        }
        if ("delimiter" in params) {
            set(delimiter, params["delimiter"])
        }
        if ("url" in params) {
            set(url, params["url"])
        }
        if ("comments" in params) {
            set(comments, params["comments"])
        }
        if ("collect_type" in params) {
            set(collectType, FileCollectType.valueOf(params["collect_type"] ?: ""))
        }
        if ("qualified" in params) {
            set(qualified, params["qualified"].equals("on"))
        }
        if ("analyze" in params) {
            set(analyze, params["analyze"].equals("on"))
        }
        if ("load" in params) {
            set(load, params["load"].equals("on"))
        }
    }

    @Throws(IllegalArgumentException::class, NumberFormatException::class, NoSuchElementException::class)
    fun updateSourceTable(username: String, params: Map<String, String?>): Long {
        val runId = params["runId"]
            ?.toLong()
            ?: throw IllegalArgumentException("runId must be a non-null parameter in the url")
        val stOid = params["stOid"]
            ?.toLong()
            ?: throw IllegalArgumentException("stOid must be a non-null parameter in the url")
        if (!PipelineRuns.checkUserRun(runId, username)) {
            throw IllegalArgumentException("Username does not own the runId")
        }
        DatabaseConnection.database.update(this) {
            buildAssignmentFromParams(params)
            where { it.stOid eq stOid }
        }
        return stOid
    }

    @Throws(IllegalArgumentException::class, NumberFormatException::class, NoSuchElementException::class)
    fun insertSourceTable(username: String, params: Map<String, String?>): Long {
        val runId = params["runId"]
            ?.toLong()
            ?: throw IllegalArgumentException("runId must be a non-null parameter in the url")
        if (!PipelineRuns.checkUserRun(runId, username)) {
            throw IllegalArgumentException("Username does not own the runId")
        }
        return DatabaseConnection.database.insertReturning(this, stOid) {
            buildAssignmentFromParams(params)
            set(SourceTables.runId, runId)
        } ?: throw IllegalArgumentException("Error while trying to insert record. Null returned")
    }

    @Throws(IllegalArgumentException::class, NumberFormatException::class, NoSuchElementException::class)
    fun deleteSourceTable(username: String, params: Map<String, String?>): Long {
        val runId = params["runId"]
            ?.toLong()
            ?: throw IllegalArgumentException("runId must be a non-null parameter in the url")
        val stOid = params["stOid"]
            ?.toLong()
            ?: throw IllegalArgumentException("stOid must be a non-null parameter in the url")
        if (!PipelineRuns.checkUserRun(runId, username)) {
            throw IllegalArgumentException("Username does not own the runId")
        }
        DatabaseConnection.database.delete(this) {
            it.stOid eq stOid
        }
        return stOid
    }

    @Throws(IllegalArgumentException::class, NumberFormatException::class, NoSuchElementException::class)
    fun alterSourceTableRecord(username: String, params: Map<String, String>): Long {
        val method = params["method"]
            ?: throw IllegalArgumentException("Method of operation on SourceTables record is not specified")
        val runId = params["runId"]
            ?.toLong()
            ?: throw IllegalArgumentException("runId must be a parameter in the url")
        if (!PipelineRuns.checkUserRun(runId, username)) {
            throw IllegalArgumentException("Username does not own the runId")
        }
        val stOid = params["stOid"]
            ?.toLong()
            ?: 0
        return DatabaseConnection.database.let { database ->
            val tableName = params["table_name"]
                ?: throw IllegalArgumentException("table name must not be empty or null")
            val fileId = params["file_id"] ?: throw IllegalArgumentException("file ID must not be empty or null")
            val fileName = params["file_name"] ?: throw IllegalArgumentException("filename must not be empty or null")
            val collectType = FileCollectType.valueOf(params["collect_type"] ?: "")
            val qualified = params["qualified"].equals("on")
            val analyze = params["analyze"].equals("on")
            val load = params["load"].equals("on")
            val fileExtension = "(?<=\\.)[^.]+\$".toRegex().find(fileName)?.value
                ?: throw IllegalArgumentException("file extension could not be found")
            val loaderType = LoaderType.values().first { fileExtension in it.extensions }
            val subTable = if (loaderType == LoaderType.MDB || loaderType == LoaderType.Excel) {
                params["sub_table"] ?: throw IllegalArgumentException("sub table must not be empty or null")
            } else {
                null
            }
            when (method) {
                "insert" -> {
                    database.insertReturning(this, this.stOid) {
                        set(this@SourceTables.runId, runId)
                        set(sourceTableName, tableName)
                        set(this@SourceTables.fileId, fileId)
                        set(this@SourceTables.fileName, fileName)
                        set(this@SourceTables.subTable, subTable)
                        set(this@SourceTables.loaderType, loaderType)
                        set(delimiter, params["delimiter"])
                        set(url, params["url"])
                        set(comments, params["comments"])
                        set(this@SourceTables.collectType, collectType)
                        set(this@SourceTables.qualified, qualified)
                        set(this@SourceTables.analyze, analyze)
                        set(this@SourceTables.load, load)
                        set(encoding, "utf8")
                    } ?: throw IllegalArgumentException("Failed to insert a new source table record")
                }
                "update" -> {
                    database.update(this) {
                        set(sourceTableName, tableName)
                        set(this@SourceTables.fileId, fileId)
                        set(this@SourceTables.fileName, fileName)
                        set(this@SourceTables.subTable, subTable)
                        set(this@SourceTables.loaderType, loaderType)
                        set(delimiter, params["delimiter"])
                        set(url, params["url"])
                        set(comments, params["comments"])
                        set(this@SourceTables.collectType, collectType)
                        set(this@SourceTables.qualified, qualified)
                        set(this@SourceTables.analyze, analyze)
                        set(this@SourceTables.load, load)
                        where { it.stOid eq stOid }
                    }
                    stOid
                }
                "delete" -> {
                    database.delete(this) {
                        it.stOid eq stOid
                    }
                    stOid
                }
                else -> throw IllegalArgumentException("Parameter 'method' must be insert, update or delete")
            }
        }
    }
}