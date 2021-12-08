@file:Suppress("MatchingDeclarationName")
package me.geoflow.core.loading

import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.reduce
import org.postgresql.copy.CopyManager
import java.io.File


/** */
class CsvParserException(t: Throwable): Throwable(t.message, t)

/**
 * Utility function to use a parser like a closable object.
 *
 * Performs the suspending [block] wrapped in a  try-catch-finally with the given context of a [CsvParser]. Function
 * starts parsing before the [block] and always stops the parser before function exit in finally block.
 */
@Suppress("TooGenericExceptionCaught")
private suspend fun <T> CsvParser.use(file: File, block: suspend CsvParser.(CsvParser) -> T): T {
    try {
        beginParsing(file)
        return block(this)
    } catch (e: Throwable) {
        throw CsvParserException(e)
    } finally {
        stopParsing()
    }
}

/**
 * Uses a [CsvParser] to parse flat file into records and analyze the resulting columns.
 *
 * Generates a chunked sequence of 10000 records per chunk to analyze and reduce to a single [AnalyzeResult]. Requires
 * that standard flat file properties are provided.
 */
suspend fun analyzeFlatFile(
    flatFile: File,
    analyzer: AnalyzeInfo,
): AnalyzeResult {
    val parserSettings = CsvParserSettings().apply {
        format.delimiter = analyzer.delimiter
        format.quote = if (analyzer.qualified) '"' else '\u0000'
        format.quoteEscape = format.quote
    }
    return CsvParser(parserSettings).use(flatFile) { parser ->
        val header = parser.parseNext().map { formatColumnName(it) }
        generateSequence { parser.parseNext() }
            .chunked(DEFAULT_CHUNK_SIZE)
            .asFlow()
            .flowOn(Dispatchers.IO)
            .map { recordChunk -> analyzeNonTypedRecords(analyzer.tableName, header, recordChunk) }
            .reduce { acc, analyzeResult -> acc.merge(analyzeResult) }
    }
}

/**
 * Extension function uses a [loader] to allow for a CopyManager to load a [flatFile] line by line to a given table.
 *
 * Creates a [CopyIn][org.postgresql.copy.CopyIn] instance then utilizes the provided stream to write each line of the
 * file to the Connection of the CopyManager.
 */
suspend fun CopyManager.loadFlatFile(
    flatFile: File,
    loader: LoadingInfo,
) {
    val copyStream = copyIn(
        getCopyCommand(
            tableName = loader.tableName,
            header = true,
            delimiter = loader.delimiter,
            qualified = loader.qualified,
            columnNames = loader.columns,
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
    fileLoadingLogger.info("Copy stream closed. Wrote $recordCount records to the target table ${loader.tableName}")
}
