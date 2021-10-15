package data_loader

import com.linuxense.javadbf.DBFReader
import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.apache.poi.ss.usermodel.*
import org.postgresql.copy.CopyManager
import org.postgresql.jdbc.PgConnection
import orm.enums.LoaderType
import java.io.File
import java.lang.Integer.min
import java.sql.Connection
import java.sql.DriverManager
import kotlin.math.floor
import java.sql.ResultSet
import java.sql.Types
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.use
import kotlin.math.max
import kotlin.text.toByteArray

private const val defaultDelimiter = ','
private val logger = KotlinLogging.logger {}
private val jdbcTypeNames = Types::class.java.fields.associate { (it.get(null) as Int) to it.name  }

data class ColumnStats(val name: String, val minLength: Int, val maxLength: Int, val type: String = "")

data class AnalyzeResult(
    val tableName: String,
    val recordCount: Int,
    val columns: List<ColumnStats>,
) {
    fun merge(analyzeResult: AnalyzeResult): AnalyzeResult {
        return copy(
            recordCount = recordCount + analyzeResult.recordCount,
            columns = columns.map { columnStats ->
                val currentStats = analyzeResult.columns.first { columnStats.name == it.name }
                columnStats.copy(
                    name = columnStats.name,
                    maxLength = max(columnStats.maxLength, currentStats.maxLength),
                    minLength = min(columnStats.minLength, currentStats.minLength),
                )
            },
        )
    }
}

private fun getCopyCommand(
    tableName: String,
    header: Boolean,
    delimiter: Char = defaultDelimiter,
    qualified: Boolean = true,
) = """
    COPY ${tableName.lowercase()}
    FROM STDIN
    WITH (
        FORMAT csv,
        DELIMITER '$delimiter',
        HEADER $header
        ${if (qualified) ", QUOTE '\"', ESCAPE '\"'" else ""}
    )
""".trimIndent()

private fun formatObject(value: Any?): String {
    return when(value) {
        null -> ""
        is Boolean -> if (value) "TRUE" else "FALSE"
        is String -> value
        is Instant -> value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)
        is LocalDateTime -> value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        is LocalDate -> value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        is LocalTime -> value.format(DateTimeFormatter.ISO_LOCAL_TIME)
        is Date -> value.toInstant().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)
        else -> value.toString()
    }
}

private fun recordToCsvBytes(record: Array<String>): ByteArray {
    return record.joinToString(separator = "\",\"", prefix = "\"", postfix = "\"\n") { value ->
        value.replace("\"", "\"\"")
    }.toByteArray()
}

private fun formatColumnName(name: String): String {
    return name.trim()
        .replace("#", "NUM")
        .replace("\\s+".toRegex(), "_")
        .uppercase()
        .replace("\\W".toRegex(), "")
        .replace("^\\d".toRegex()) {
            "_${it.value}"
        }
        .take(60)
}

private suspend fun <T> CsvParser.use(file: File, func: suspend CsvParser.(CsvParser) -> T): T {
    try {
        beginParsing(file)
        return func(this)
    } catch (e: Throwable) {
        throw e
    } finally {
        stopParsing()
    }
}

@JvmName("analyzeStringRecords")
private fun analyzeRecords(
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

private fun analyzeRecords(
    tableName: String,
    header: List<Pair<String, String>>,
    records: List<Array<String>>,
): AnalyzeResult {
    require(records.isNotEmpty()) { "Records to analyze cannot be empty" }
    require(header.size == records.first().size) { "First record size must match header size" }
    var recordCount = 0
    val stats = header.mapIndexed { index, (name, type) ->
        val lengths = records.map { record ->
            record[index].length
        }
        if (recordCount == 0) {
            recordCount = lengths.size
        }
        ColumnStats(
            name = name,
            maxLength = lengths.maxOf { it },
            minLength = lengths.minOf { it },
            type = type,
        )
    }
    return AnalyzeResult(tableName, recordCount, stats)
}

fun Connection.checkTableExists(tableName: String, schema: String = "public"): Boolean {
    return prepareStatement("""
        select table_name
        from   information_schema.tables
        where  table_schema = ?
        and    table_name = ?
    """.trimIndent())
        .apply {
            setString(1, schema)
            setString(2, tableName.lowercase())
        }
        .use { statement ->
            statement.executeQuery().use { rs ->
                rs.next()
            }
        }
}

@Throws(IllegalArgumentException::class)
suspend fun Connection.loadFile(
    file: File,
    tableNames: List<String>,
    subTableNames: List<String> = listOf(),
    delimiter: Char = ',',
    qualified: Boolean = true,
) {
    require(file.exists()) { "File cannot be found" }
    require(file.isFile) { "File object provided is not a file in the directory system" }
    require(tableNames.isNotEmpty()) { "Table names cannot be empty" }
    val loaderType = LoaderType
        .values()
        .firstOrNull { file.extension in it.extensions }
        ?: throw IllegalArgumentException("File must be of a supported data format")
    with(CopyManager(this.unwrap(PgConnection::class.java))) {
        when(loaderType) {
            LoaderType.Excel -> {
                require(tableNames.size == subTableNames.size) {
                    "Number of table names must match number of sheet names"
                }
                loadExcelFile(
                    excelFile = file,
                    tableNames = tableNames,
                    sheetNames = subTableNames,
                    connection = this@loadFile,
                )
            }
            LoaderType.Flat -> {
                loadFlatFile(
                    flatFile = file,
                    tableName = tableNames.first(),
                    delimiter = delimiter,
                    qualified = qualified,
                )
            }
            LoaderType.MDB -> {
                require(tableNames.size == subTableNames.size) {
                    "Number of table names must match number of mdb table names"
                }
                loadMdbFile(
                    mdbFile = file,
                    tableNames = tableNames,
                    mdbTableNames = subTableNames,
                    connection = this@loadFile,
                )
            }
            LoaderType.DBF -> {
                loadDbfFile(
                    dbfFile = file,
                    tableName = tableNames.first(),
                )
            }
        }
    }
    commit()
}

@Throws(IllegalArgumentException::class)
suspend fun analyzeFile(
    file: File,
    tableNames: List<String>,
    subTableNames: List<String> = listOf(),
    delimiter: Char = ',',
    qualified: Boolean = true,
): Flow<AnalyzeResult> {
    require(file.exists()) { "File cannot be found" }
    require(file.isFile) { "File object provided is not a file in the directory system" }
    require(tableNames.isNotEmpty()) { "Table names cannot be empty" }
    val loaderType = LoaderType
        .values()
        .firstOrNull { file.extension in it.extensions }
        ?: throw IllegalArgumentException("File must be of a supported data format")
    return when(loaderType) {
        LoaderType.Excel -> {
            analyzeExcelFile(
                excelFile = file,
                tableNames = tableNames,
                sheetNames = subTableNames,
            )
        }
        LoaderType.Flat -> {
            flow {
                val result = analyzeFlatFile(
                    flatFile = file,
                    tableName = tableNames.first(),
                    delimiter = delimiter,
                    qualified = qualified,
                )
                emit(result)
            }
        }
        LoaderType.MDB -> {
            analyzeMdbFile(
                mdbFile = file,
                tableNames = tableNames,
                mdbTableNames = subTableNames,
            )
        }
        LoaderType.DBF -> {
            flow {
                val result = analyzeDbfFile(
                    dbfFile = file,
                    tableName = tableNames.first(),
                )
                emit(result)
            }
        }
    }
}

private suspend fun analyzeFlatFile(
    flatFile: File,
    tableName: String,
    delimiter: Char,
    qualified: Boolean
): AnalyzeResult {
    val parserSettings = CsvParserSettings().apply {
        format.delimiter = delimiter
        format.quote = if (qualified) '"' else '\u0000'
        format.quoteEscape = format.quote
    }
    return CsvParser(parserSettings).use(flatFile) { parser ->
        val header = parser.parseNext().map { formatColumnName(it) }
        generateSequence { parser.parseNext() }
            .chunked(10000)
            .asFlow()
            .map { recordChunk -> analyzeRecords(tableName, header, recordChunk) }
            .reduce { acc, analyzeResult -> acc.merge(analyzeResult) }
    }
}

private suspend fun CopyManager.loadFlatFile(
    flatFile: File,
    tableName: String,
    delimiter: Char,
    qualified: Boolean,
) {
    val copyStream = copyIn(
        getCopyCommand(
            tableName = tableName,
            header = true,
            delimiter = delimiter,
            qualified = qualified,
        )
    )
    flatFile
        .bufferedReader()
        .useLines { lines ->
            lines.asFlow()
                .flowOn(Dispatchers.IO)
                .map { line -> "$line\n".toByteArray() }
                .collect {
                    copyStream.writeToCopy(it, 0, it.size)
                }
        }
    val recordCount = copyStream.endCopy()
    logger.info("Copy stream closed. Wrote $recordCount records to the target table $tableName")
}

private fun Sheet.excelSheetRecords(
    headerLength: Int,
    evaluator: FormulaEvaluator,
    formatter: DataFormatter,
): Sequence<Array<String>> {
    val iterator = rowIterator()
    iterator.next()
    return iterator
        .asSequence()
        .map { row ->
            0.until(headerLength)
                .map { row.getCell(it, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK) }
                .map { cell ->
                    val cellValue = evaluator.evaluate(cell) ?: CellValue("")
                    when (cellValue.cellType) {
                        null ->
                            ""
                        CellType.NUMERIC -> {
                            val numValue = cellValue.numberValue
                            when {
                                DateUtil.isCellDateFormatted(cell) ->
                                    cell.localDateTimeCellValue.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                floor(numValue) == numValue -> numValue.toLong().toString()
                                else -> numValue.toString()
                            }
                        }
                        CellType.STRING -> cellValue.stringValue ?: ""
                        CellType.BLANK -> ""
                        CellType.BOOLEAN -> if (cellValue.booleanValue) "TRUE" else "FALSE"
                        CellType._NONE, CellType.ERROR -> formatter.formatCellValue(cell)
                        else -> ""
                    }.trim()
                }
                .toList()
                .toTypedArray()
        }
}

private suspend fun analyzeExcelFile(
    excelFile: File,
    tableNames: List<String>,
    sheetNames: List<String>,
) = flow {
    excelFile.inputStream().use { inputStream ->
        withContext(Dispatchers.IO) {
            @Suppress("BlockingMethodInNonBlockingContext")
            WorkbookFactory.create(inputStream)
        }.use { workbook ->
            val formulaEvaluator = workbook.creationHelper.createFormulaEvaluator()
            val dataFormatter = DataFormatter()
            (sheetNames zip tableNames).forEach { (sheetName, tableName) ->
                val sheet: Sheet = workbook.getSheet(sheetName) ?: throw IllegalStateException("Could not find sheet")
                val header = sheet.first().cellIterator().asSequence().map { cell ->
                    formatColumnName(cell.stringCellValue)
                }.toList()
                val analyzeResult = sheet.excelSheetRecords(header.size, formulaEvaluator, dataFormatter)
                    .chunked(10000)
                    .asFlow()
                    .flowOn(Dispatchers.IO)
                    .map { recordChunk -> analyzeRecords(tableName, header, recordChunk) }
                    .reduce { acc, analyzeResult -> acc.merge(analyzeResult) }
                emit(analyzeResult)
            }
        }
    }
}

private suspend fun CopyManager.loadExcelFile(
    excelFile: File,
    tableNames: List<String>,
    sheetNames: List<String>,
    connection: Connection,
) {
    excelFile.inputStream().use { inputStream ->
        withContext(Dispatchers.IO) {
            @Suppress("BlockingMethodInNonBlockingContext")
            WorkbookFactory.create(inputStream)
        }.use { workbook ->
            val formulaEvaluator = workbook.creationHelper.createFormulaEvaluator()
            val dataFormatter = DataFormatter()
            (tableNames zip sheetNames).forEach { (tableName, sheetName) ->
                val sheet = workbook.getSheet(sheetName)
                val copyStream = copyIn(
                    getCopyCommand(
                        tableName = tableName,
                        header = true,
                    )
                )
                val headerLength = sheet.first().physicalNumberOfCells
                sheet.excelSheetRecords(headerLength, formulaEvaluator, dataFormatter)
                    .asFlow()
                    .flowOn(Dispatchers.IO)
                    .map { record -> recordToCsvBytes(record) }
                    .collect {
                        copyStream.writeToCopy(it, 0, it.size)
                    }
                val recordCount = copyStream.endCopy()
                connection.commit()
                logger.info("Copy stream closed. Wrote $recordCount records to the target table $tableName")
            }
        }
    }
}

private fun ResultSet.resultRecords(): Sequence<Array<String>> {
    return generateSequence {
        if (next()) {
            (1..metaData.columnCount).map { column ->
                formatObject(getObject(column)).replace("\"", "\"\"")
            }.toTypedArray()
        } else {
            null
        }
    }
}

private fun analyzeMdbFile(
    mdbFile: File,
    tableNames: List<String>,
    mdbTableNames: List<String>,
) = flow {
    DriverManager
        .getConnection("jdbc:ucanaccess://${mdbFile.absolutePath}").use { connection ->
            mdbTableNames.zip(tableNames).forEach { (mdbTableName, tableName) ->
                connection
                    .prepareStatement("SELECT * FROM $mdbTableName")
                    .executeQuery()
                    .use { rs ->
                        val headers = (1..rs.metaData.columnCount).map {
                            Pair(
                                formatColumnName(rs.metaData.getColumnName(it)),
                                (jdbcTypeNames[rs.metaData.getColumnType(it)] ?: "")
                            )
                        }
                        val analyzeResult = rs.resultRecords()
                            .chunked(10000)
                            .asFlow()
                            .flowOn(Dispatchers.IO)
                            .map { records -> analyzeRecords(tableName, headers, records) }
                            .reduce { acc, analyzeResult -> acc.merge(analyzeResult) }
                        emit(analyzeResult)
                    }
            }
        }
}

private suspend fun CopyManager.loadMdbFile(
    mdbFile: File,
    tableNames: List<String>,
    mdbTableNames: List<String>,
    connection: Connection,
) {
    DriverManager
        .getConnection("jdbc:ucanaccess://${mdbFile.absolutePath}").use { mdbConnection ->
            (tableNames zip mdbTableNames).forEach { (tableName, mdbTableName) ->
                val copyStream = copyIn(
                    getCopyCommand(
                        tableName = tableName,
                        header = false,
                    )
                )
                mdbConnection
                    .prepareStatement("SELECT * FROM $mdbTableName")
                    .executeQuery()
                    .use { rs ->
                        rs.resultRecords()
                            .asFlow()
                            .flowOn(Dispatchers.IO)
                            .map { record -> recordToCsvBytes(record) }
                            .collect {
                                copyStream.writeToCopy(it, 0, it.size)
                            }
                    }
                val recordCount = copyStream.endCopy()
                connection.commit()
                logger.info("Copy stream closed. Wrote $recordCount records to the target table $tableName")
            }
        }
}

private fun getDbfHeader(dbfFile: File): List<Pair<String, String>> {
    return dbfFile.inputStream().use { inputStream ->
        DBFReader(inputStream).use { reader ->
            0.until(reader.fieldCount).map {
                val field = reader.getField(it)
                field.name to field.type.name
            }
        }
    }
}

private fun dbfFileRecords(dbfFile: File) = sequence {
    dbfFile.inputStream().use { inputStream ->
        DBFReader(inputStream).use { reader ->
            generateSequence {
                reader.nextRecord()
            }.forEach { record ->
                yield(
                    record.map { value ->
                        formatObject(value)
                    }.toTypedArray()
                )
            }
        }
    }
}

private suspend fun analyzeDbfFile(
    dbfFile: File,
    tableName: String,
): AnalyzeResult {
    val header = getDbfHeader(dbfFile)
    return dbfFileRecords(dbfFile)
        .chunked(10000)
        .asFlow()
        .flowOn(Dispatchers.IO)
        .map { records -> analyzeRecords(tableName, header, records) }
        .reduce { acc, analyzeResult -> acc.merge(analyzeResult) }
}

private suspend fun CopyManager.loadDbfFile(
    dbfFile: File,
    tableName: String,
) {
    val copyStream = copyIn(
        getCopyCommand(
            tableName = tableName,
            header = false,
        )
    )
    dbfFileRecords(dbfFile)
        .asFlow()
        .flowOn(Dispatchers.IO)
        .map {record -> recordToCsvBytes(record) }
        .collect {
            copyStream.writeToCopy(it, 0, it.size)
        }
    val recordCount = copyStream.endCopy()
    logger.info("Copy stream closed. Wrote $recordCount records to the target table $tableName")
}
