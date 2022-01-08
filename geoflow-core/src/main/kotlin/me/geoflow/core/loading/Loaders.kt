@file:Suppress("TooManyFunctions")
package me.geoflow.core.loading

import me.geoflow.core.database.enums.LoaderType
import me.geoflow.core.database.extensions.queryFirstOrNull
import me.geoflow.core.database.extensions.queryHasResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import mu.KLogger
import mu.KotlinLogging
import org.postgresql.copy.CopyManager
import org.postgresql.jdbc.PgConnection
import me.geoflow.core.utils.requireNotEmpty
import java.io.File
import java.io.InputStream
import java.math.BigDecimal
import java.sql.Connection
import java.sql.Time
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date

/** postgresql column max length is 63 bytes so 60 characters should be good */
const val MAX_COLUMN_NAME_LENGTH: Int = 60
/** default chunk size while analyzing a file */
const val DEFAULT_CHUNK_SIZE: Int = 10_000
/** default delimiter of flat files, generally the delimiter is comma */
const val DEFAULT_DELIMITER: Char = ','
/** logger for file loading operations */
val fileLoadingLogger: KLogger = KotlinLogging.logger {}
/** Map of jdbc type constants to the name they represent */
val jdbcTypeNames: Map<Int, String> = Types::class.java.fields.associate { (it.get(null) as Int) to it.name  }

/**
 * Obtain the Postgresql COPY command for the specified [tableName] through a stream with various format options. The
 * byte stream will always be CSV file like with a specified [delimiter] and a possible [header] line. There is also
 * an option for non-qualified files where the QUOTE nad ESCAPE options are not set.
 */
fun getCopyCommand(
    tableName: String,
    header: Boolean,
    columnNames: List<String>,
    delimiter: Char = DEFAULT_DELIMITER,
    qualified: Boolean = true,
): String {
    return """
        COPY ${tableName.lowercase()} (${columnNames.joinToString()})
        FROM STDIN
        WITH (
            FORMAT csv,
            DELIMITER '$delimiter',
            HEADER $header,
            NULL ''
            ${if (qualified) ", QUOTE '\"', ESCAPE '\"'" else ""}
        )
    """.trimIndent()
}

/** Accepts a nullable object and formats the value to string. Most of the formatting is for Date-like types */
@Suppress("ComplexMethod")
fun formatObject(value: Any?): String {
    return when(value) {
        null -> ""
        is Boolean -> if (value) "TRUE" else "FALSE"
        is String -> value
        is BigDecimal -> value.toPlainString()
        is ByteArray -> value.decodeToString()
        is Timestamp -> value.toInstant().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)
        is Instant -> value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)
        is LocalDateTime -> value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        is LocalDate -> value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        is LocalTime -> value.format(DateTimeFormatter.ISO_LOCAL_TIME)
        is Time -> value.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME)
        is Date -> value.toInstant().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)
        else -> value.toString()
    }.trim()
}

/** Converts an array of String values to a ByteArray that reflects a CSV record. Used to pipe output to COPY command */
fun recordToCsvBytes(record: Array<String>): ByteArray {
    return record.joinToString(separator = "\",\"", prefix = "\"", postfix = "\"\n") { value ->
        value.replace("\"", "\"\"")
    }.toByteArray()
}

/** Transforms a source column name to a valid Postgresql column name */
fun formatColumnName(name: String): String {
    return name.trim()
        .replace("#", "NUM")
        .replace("\\s+".toRegex(), "_")
        .uppercase()
        .replace("\\W".toRegex(), "")
        .replace("^\\d".toRegex()) { "_${it.value}" }
        .take(MAX_COLUMN_NAME_LENGTH)
}

/** Analyzes records for files without defined column types. Calls [analyzeRecords] with the default type of VARCHAR */
fun analyzeNonTypedRecords(
    tableName: String,
    header: List<String>,
    records: List<Array<String>>,
): AnalyzeResult {
    return analyzeRecords(
        tableName,
        header.map { it to "VARCHAR" },
        records,
    )
}

/**
 * Analyzes a [records] chunk given a [tableName] and [header] list, returning an [AnalyzeResult].
 *
 * The result is obtained by using passed data and looking into each column and finding the max and min string lengths.
 */
fun analyzeRecords(
    tableName: String,
    header: List<Pair<String, String>>,
    records: List<Array<String>>,
): AnalyzeResult {
    require(records.isNotEmpty()) { "Records to analyze cannot be empty" }
    require(header.size == records.first().size) { "First record size must match header size" }
    val recordCount = records.size
    val stats = header.mapIndexed { index, (name, type) ->
        val lengths = records.map { record ->
            record[index].length
        }.sorted()
        ColumnStats(
            name = name,
            maxLength = lengths.last(),
            minLength = lengths.first(),
            type = type,
            index = index,
        )
    }
    return AnalyzeResult(tableName, recordCount, stats)
}

/** Running SQL query to find out if the [tableName] can be found in the given [schema] */
fun Connection.checkTableExists(tableName: String, schema: String = "public"): Boolean {
    val sql = """
        select table_name
        from   information_schema.tables
        where  table_schema = ?
        and    table_name = ?
    """.trimIndent()
    return queryHasResult(sql, schema, tableName.lowercase())
}

/** Query database to get an aggregated string of column names for default data loading. Skips generated fields */
private fun Connection.getDefaultDataColumnNames(tableName: String): String {
    val sql = """
        select string_agg(column_name, ',' order by ordinal_position) "columns"
        from   information_schema.columns
        where  table_name = ?
        and    is_identity = 'NO'
        group by table_name;
    """.trimIndent()
    return queryFirstOrNull(sql = sql, tableName) ?: throw IllegalArgumentException("Table does not exist")
}

/** Loads default data into the [tableName] specified using the [inputStream] as a CSV data stream */
fun Connection.loadDefaultData(tableName: String, inputStream: InputStream): Long {
    return CopyManager(this.unwrap(PgConnection::class.java))
        .copyIn(
            getCopyCommand(
                tableName = tableName,
                header = true,
                columnNames = getDefaultDataColumnNames(tableName).split(','),
            ),
            inputStream
        )
}

/**
 * Loads a given [file] into the Connection, if the file type is supported.
 *
 * Checks some assumptions (see Throws), creates a [CopyManager] and then calls the appropriate extension function to
 * load the given file type. Loading is performed by reading and transforming each file's record into a [ByteArray] to
 * stream those bytes to the Connection server. For more details or loading requirement per [LoaderType] see the
 * appropriate loader functions.
 *
 * @throws IllegalArgumentException various cases:
 * - [file] does not exist
 * - [file] provided is not a file
 * - [loaders] is empty
 * - [LoaderType] cannot be found
 */
suspend fun Connection.loadFile(
    file: File,
    loaders: List<LoadingInfo>,
) {
    require(file.exists()) { "File cannot be found" }
    require(file.isFile) { "File object provided is not a file in the directory system" }
    requireNotEmpty(loaders) { "Loaders cannot be empty" }
    val loaderType = LoaderType.getLoaderTypeFromExtension(file.extension)
    with(CopyManager(this.unwrap(PgConnection::class.java))) {
        when(loaderType) {
            LoaderType.Excel -> {
                loadExcelFile(
                    excelFile = file,
                    loaders = loaders,
                )
            }
            LoaderType.Flat -> {
                loadFlatFile(
                    flatFile = file,
                    loader = loaders.first(),
                )
            }
            LoaderType.MDB -> {
                loadMdbFile(
                    mdbFile = file,
                    loaders = loaders,
                )
            }
            LoaderType.DBF -> {
                loadDbfFile(
                    dbfFile = file,
                    loader = loaders.first(),
                )
            }
        }
    }
}

/**
 * Returns a Flow that emits [AnalyzeResult] instances for a given [file] if the file type is supported.
 *
 * Checks some assumptions (see Throws) then calls the appropriate extension function to analyze the given file type.
 * Analyzing is performed by reading and transforming each file's records into chunks then provides metadata and stats
 * on the file's columns. For more details or analyzing requirement per [LoaderType] see the appropriate analysis
 * functions.
 *
 * Since files can contain multiple sub tables, those analysis functions are an extension of [FlowCollector] in order
 * to process and emit results directly within the function.
 *
 * @throws IllegalArgumentException various cases:
 * - [file] does not exist
 * - [file] provided is not a file
 * - [analyzers] is empty
 * - [LoaderType] cannot be found
 */
suspend fun analyzeFile(
    file: File,
    analyzers: List<AnalyzeInfo>,
): Flow<AnalyzeResult> {
    require(file.exists()) { "File cannot be found" }
    require(file.isFile) { "File object provided is not a file in the directory system" }
    requireNotEmpty(analyzers) { "analyzers cannot be empty" }
    val loaderType = LoaderType.getLoaderTypeFromExtension(file.extension)
    return flow {
        when(loaderType) {
            LoaderType.Excel -> {
                analyzeExcelFile(
                    excelFile = file,
                    analyzers = analyzers,
                )
            }
            LoaderType.Flat -> {
                val result = analyzeFlatFile(
                    flatFile = file,
                    analyzer = analyzers.first(),
                )
                emit(result)
            }
            LoaderType.MDB -> {
                analyzeMdbFile(
                    mdbFile = file,
                    analyzers = analyzers,
                )
            }
            LoaderType.DBF -> {
                val result = analyzeDbfFile(
                    dbfFile = file,
                    analyzer = analyzers.first(),
                )
                emit(result)
            }
        }
    }
}
