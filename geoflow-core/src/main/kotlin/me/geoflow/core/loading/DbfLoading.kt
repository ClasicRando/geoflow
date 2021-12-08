package me.geoflow.core.loading

import com.linuxense.javadbf.DBFReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.reduce
import me.geoflow.core.utils.mapToArray
import org.postgresql.copy.CopyManager
import java.io.File

/**
 * Extract header details from a DBF [file][dbfFile] and return as pairs. Note: avoided a Map to preserve order
 */
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

/**
 * Utility Function to extract records from a DBF [file][dbfFile] as a yielded sequence of String arrays
 */
private fun dbfFileRecords(dbfFile: File) = sequence {
    dbfFile.inputStream().use { inputStream ->
        DBFReader(inputStream).use { reader ->
            for (record in generateSequence { reader.nextRecord() }) {
                yield(record.mapToArray { value -> formatObject(value) })
            }
        }
    }
}


/**
 * Extension function to analyze the provided [file][dbfFile].
 *
 * Generates a chunked sequence of 10000 records per chunk to analyze and reduce to a single [AnalyzeResult].
 */
suspend fun analyzeDbfFile(
    dbfFile: File,
    analyzer: AnalyzeInfo,
): AnalyzeResult {
    val header = getDbfHeader(dbfFile)
    return dbfFileRecords(dbfFile)
        .chunked(DEFAULT_CHUNK_SIZE)
        .asFlow()
        .flowOn(Dispatchers.IO)
        .map { records -> analyzeRecords(analyzer.tableName, header, records) }
        .reduce { acc, analyzeResult -> acc.merge(analyzeResult) }
}

/**
 * Extension function uses a [loader] to allow for a CopyManager to load a [file][dbfFile]line by line to a given table.
 *
 * Creates a [CopyIn][org.postgresql.copy.CopyIn] instance then utilizes the provided stream to write each record of the
 * file to the Connection of the CopyManager.
 */
suspend fun CopyManager.loadDbfFile(
    dbfFile: File,
    loader: LoadingInfo,
) {
    val copyStream = copyIn(
        getCopyCommand(
            tableName = loader.tableName,
            header = false,
            columnNames = loader.columns,
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
    fileLoadingLogger.info("Copy stream closed. Wrote $recordCount records to the target table ${loader.tableName}")
}
