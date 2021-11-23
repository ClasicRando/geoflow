package loading

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.reduce
import org.postgresql.copy.CopyManager
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

/** Alias for a header information (name and type). Keep as list to retain order */
typealias HeaderData = List<Pair<String, String>>

/**
 * Extension function to yield rows as an array of Strings
 */
private fun ResultSet.resultRecords() = sequence {
    while (next()) {
        val row = mutableListOf<String>()
        for (i in 1..metaData.columnCount) {
            row += formatObject(getObject(i)).replace("\"", "\"\"")
        }
        yield(row.toTypedArray())
    }
}

/**
 * Executes the provided [sql] query and gives access to the resulting records as a sequence of string arrays. Upon
 * completion of the provided [block], the [PreparedStatement][java.sql.PreparedStatement] and [ResultSet]
 * are closed by utilizing the [use][kotlin.use] function.
 */
private suspend fun Connection.useResultRecords(
    sql: String,
    block: suspend ResultSet.(Sequence<Array<String>>) -> Unit,
) {
    prepareStatement(sql).use { statement ->
        statement.executeQuery().use { rs ->
            rs.block(rs.resultRecords())
        }
    }
}

/** Extension property that gets the header data for a [ResultSet] */
private val ResultSet.headers: HeaderData
    get() = buildList {
        for (index in 1..metaData.columnCount) {
            val item = Pair(
                formatColumnName(metaData.getColumnName(index)),
                jdbcTypeNames.getOrDefault(metaData.getColumnType(index), "")
            )
            add(item)
        }
    }

/**
 * Extension function to analyze all the required sub tables (as per [analyzers]).
 *
 * Generates a chunked sequence of 10000 records per chunk to analyze and reduce to a single [AnalyzeResult].
 */
suspend fun FlowCollector<AnalyzeResult>.analyzeMdbFile(
    mdbFile: File,
    analyzers: List<AnalyzeInfo>,
) {
    DriverManager.getConnection("jdbc:ucanaccess://${mdbFile.absolutePath}").use { connection ->
        for (info in analyzers) {
            connection.useResultRecords("SELECT * FROM ${info.subTable!!}") { resultRecords ->
                val headerData = headers
                val analyzeResult = resultRecords
                    .chunked(DEFAULT_CHUNK_SIZE)
                    .asFlow()
                    .flowOn(Dispatchers.IO)
                    .map { records -> analyzeRecords(info.tableName, headerData, records) }
                    .reduce { acc, analyzeResult -> acc.merge(analyzeResult) }
                emit(analyzeResult)
            }
        }
    }
}

/**
 * Extension function allows for a CopyManager to easily load a [file][mdbFile] to each table using the linked sub
 * tables.
 *
 * Creates a [CopyIn][org.postgresql.copy.CopyIn] instance for each sub table then utilizes the provided stream to write
 * each record of the sub table to the Connection of the CopyManager.
 */
suspend fun CopyManager.loadMdbFile(
    mdbFile: File,
    loaders: List<LoadingInfo>,
) {
    DriverManager.getConnection("jdbc:ucanaccess://${mdbFile.absolutePath}").use { mdbConnection ->
        for (loader in loaders) {
            val copyStream = copyIn(
                getCopyCommand(
                    tableName = loader.tableName,
                    header = false,
                    columnNames = loader.columns,
                )
            )
            mdbConnection.useResultRecords("SELECT * FROM ${loader.subTable!!}") { records ->
                records.asFlow()
                    .flowOn(Dispatchers.IO)
                    .map { record -> recordToCsvBytes(record) }
                    .collect {
                        copyStream.writeToCopy(it, 0, it.size)
                    }
            }
            val recordCount = copyStream.endCopy()
            fileLoadingLogger.info(
                "Copy stream closed. Wrote $recordCount records to the target table ${loader.tableName}"
            )
        }
    }
}
