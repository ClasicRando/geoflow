package data_loader

import com.linuxense.javadbf.DBFReader
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.apache.poi.ss.usermodel.*
import org.postgresql.copy.CopyManager
import org.postgresql.jdbc.PgConnection
import orm.enums.LoaderType
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import kotlin.math.floor
import java.sql.ResultSet
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.use
import kotlin.text.toByteArray

const val defaultDelimiter = ','
val logger = KotlinLogging.logger {}

fun getCopyCommand(
    tableName: String,
    header: Boolean,
    delimiter: Char = defaultDelimiter,
    qualified: Boolean = true,
) = """
    COPY $tableName
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

fun recordToCsvBytes(record: List<String>): ByteArray {
    return record.joinToString(separator = "\",\"", prefix = "\"", postfix = "\"\n") { value ->
        value.replace("\"", "\"\"")
    }.toByteArray()
}

@Throws(IllegalArgumentException::class)
suspend fun Connection.loadFile(
    file: File,
    tableNames: List<String>,
    subTableNames: List<String> = listOf(),
    delimiter: Char = ',',
    qualified: Boolean = true,
) {
    if (!file.exists()) {
        throw IllegalArgumentException("File cannot be found")
    }
    if (!file.isFile) {
        throw IllegalArgumentException("File object provided is not a file in the directory system")
    }
    if (tableNames.isEmpty()) {
        throw IllegalArgumentException("Table names cannot be empty")
    }
    val loaderType = LoaderType
        .values()
        .firstOrNull { file.extension in it.extensions }
        ?: throw IllegalArgumentException("File must be of a supported data format")
    with(CopyManager(this.unwrap(PgConnection::class.java))) {
        when(loaderType) {
            LoaderType.Excel -> {
                if (tableNames.size != subTableNames.size) {
                    throw IllegalArgumentException("Number of table names must match number of sheet names")
                }
                loadExcelFile(
                    excelFile = file,
                    tableNames = tableNames,
                    sheetNames = subTableNames,
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
                if (tableNames.size != subTableNames.size) {
                    throw IllegalArgumentException("Number of table names must match number of sheet names")
                }
                loadMdbFile(
                    mdbFile = file,
                    tableNames = tableNames,
                    mdbTableNames = subTableNames,
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
}

private suspend fun CopyManager.loadFlatFile(
    flatFile: File,
    tableName: String,
    delimiter: Char = defaultDelimiter,
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

private fun Sheet.excelSheetFlow(
    evaluator: FormulaEvaluator,
    formatter: DataFormatter,
) = rowIterator()
    .asFlow()
    .map { row ->
        row.cellIterator()
            .asSequence()
            .map { cell ->
                val cellValue = evaluator.evaluate(cell)
                when (cellValue.cellType) {
                    CellType.NUMERIC -> {
                        val numValue = cellValue.numberValue
                        when {
                            DateUtil.isCellDateFormatted(cell) ->
                                cell.localDateTimeCellValue.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            floor(numValue) == numValue -> numValue.toLong().toString()
                            else -> numValue.toString()
                        }
                    }
                    CellType.STRING -> cellValue.stringValue
                    CellType.BLANK -> ""
                    CellType.BOOLEAN -> if (cellValue.booleanValue) "TRUE" else "FALSE"
                    CellType._NONE, CellType.ERROR -> formatter.formatCellValue(cell)
                    else -> ""
                }.trim()
            }.toList()
    }

private suspend fun CopyManager.loadExcelFile(
    excelFile: File,
    tableNames: List<String>,
    sheetNames: List<String>,
) {
    excelFile.inputStream().use { inputStream ->
        withContext(Dispatchers.IO) {
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
                sheet.excelSheetFlow(formulaEvaluator, dataFormatter)
                    .flowOn(Dispatchers.IO)
                    .map { record -> recordToCsvBytes(record) }
                    .collect {
                        copyStream.writeToCopy(it, 0, it.size)
                    }
                val recordCount = copyStream.endCopy()
                logger.info("Copy stream closed. Wrote $recordCount records to the target table $tableName")
            }
        }
    }
}

private fun ResultSet.resultFlow(): Flow<List<String>> {
    return generateSequence {
        if (next()) {
            (1..metaData.columnCount).map { column ->
                formatObject(getObject(column)).replace("\"", "\"\"")
            }
        } else {
            null
        }
    }.asFlow()
}

private suspend fun CopyManager.loadMdbFile(
    mdbFile: File,
    tableNames: List<String>,
    mdbTableNames: List<String>,
) {
    DriverManager
        .getConnection("jdbc:ucanaccess://${mdbFile.absolutePath}").use { connection ->
            (tableNames zip mdbTableNames).forEach { (tableName, mdbTableName) ->
                val copyStream = copyIn(
                    getCopyCommand(
                        tableName = tableName,
                        header = false,
                    )
                )
                connection
                    .prepareStatement("SELECT * FROM $mdbTableName")
                    .executeQuery()
                    .use { rs ->
                        rs.resultFlow()
                            .flowOn(Dispatchers.IO)
                            .map {record -> recordToCsvBytes(record) }
                            .collect {
                                copyStream.writeToCopy(it, 0, it.size)
                            }
                    }
                copyStream.endCopy()
            }
        }
}

private suspend fun dbfFileFlow(dbfFile: File) = flow {
    dbfFile.inputStream().use { inputStream ->
        DBFReader(inputStream).use { reader ->
            generateSequence {
                reader.nextRecord()
            }.forEach { record ->
                emit(
                    record.map { value ->
                        formatObject(value)
                    }
                )
            }
        }
    }
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
    dbfFileFlow(dbfFile)
        .flowOn(Dispatchers.IO)
        .map {record -> recordToCsvBytes(record) }
        .collect {
            copyStream.writeToCopy(it, 0, it.size)
        }
    copyStream.endCopy()
}
