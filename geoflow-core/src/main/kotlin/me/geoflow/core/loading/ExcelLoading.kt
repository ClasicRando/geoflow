package me.geoflow.core.loading

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.CellValue
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.postgresql.copy.CopyManager
import java.io.File
import java.time.format.DateTimeFormatter
import kotlin.math.floor


/**
 * Extension function to extract a sequence of records from an Excel [Sheet].
 *
 * Uses the sheet's [row iterator][Sheet.rowIterator] to traverse the sheet and collect each row's cells as String.
 * Since a cell can have many types, be a formula or be null, we apply a transformation to extract the cell value and
 * convert that value to a String.
 */
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
                        null -> ""
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

/**
 * Extension function to analyze all the required sheets (as per [analyzers]).
 *
 * Generates a chunked sequence of 10000 records per chunk to analyze and reduce to a single [AnalyzeResult].
 */
suspend fun FlowCollector<AnalyzeResult>.analyzeExcelFile(
    excelFile: File,
    analyzers: List<AnalyzeInfo>,
) {
    excelFile.inputStream().use { inputStream ->
        withContext(Dispatchers.IO) {
            @Suppress("BlockingMethodInNonBlockingContext")
            WorkbookFactory.create(inputStream)
        }.use { workbook ->
            val formulaEvaluator = workbook.creationHelper.createFormulaEvaluator()
            val dataFormatter = DataFormatter()
            for (info in analyzers) {
                val sheet: Sheet = workbook.getSheet(info.subTable)
                    ?: throw IllegalStateException("Could not find sheet")
                val header = sheet.first().cellIterator().asSequence().map { cell ->
                    formatColumnName(cell.stringCellValue)
                }.toList()
                val analyzeResult = sheet.excelSheetRecords(header.size, formulaEvaluator, dataFormatter)
                    .chunked(DEFAULT_CHUNK_SIZE)
                    .asFlow()
                    .flowOn(Dispatchers.IO)
                    .map { recordChunk -> analyzeNonTypedRecords(info.tableName, header, recordChunk) }
                    .reduce { acc, analyzeResult -> acc.merge(analyzeResult) }
                emit(analyzeResult)
            }
        }
    }
}

/**
 * Extension function allows for a CopyManager to easily load a [file][excelFile] to each table using the linked sheet.
 *
 * Creates a [CopyIn][org.postgresql.copy.CopyIn] instance for each sheet then utilizes the provided stream to write
 * each record of the sheet to the Connection of the CopyManager.
 */
suspend fun CopyManager.loadExcelFile(
    excelFile: File,
    loaders: List<LoadingInfo>,
) {
    excelFile.inputStream().use { inputStream ->
        withContext(Dispatchers.IO) {
            @Suppress("BlockingMethodInNonBlockingContext")
            WorkbookFactory.create(inputStream)
        }.use { workbook ->
            val formulaEvaluator = workbook.creationHelper.createFormulaEvaluator()
            val dataFormatter = DataFormatter()
            for (loader in loaders) {
                val sheet = workbook.getSheet(loader.subTable!!)
                val copyStream = copyIn(
                    getCopyCommand(
                        tableName = loader.tableName,
                        header = true,
                        columnNames = loader.columns,
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
                fileLoadingLogger.info(
                    "Copy stream closed. Wrote $recordCount records to the target table ${loader.tableName}"
                )
            }
        }
    }
}
