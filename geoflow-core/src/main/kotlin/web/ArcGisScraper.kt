package web

import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun scrapeArcGisService(url: String, outputPath: String) {
    with(ArcGisServiceMetadata.fromUrl(url)) {
        val file = File(outputPath, "$name.csv")
        file.createNewFile()
        with(CsvWriter(file, csvSettings)) {
            writeHeaders()
            fetchQueries()
                .buffer()
                .collect { tempFile ->
                    CsvParser(csvParserSettings)
                        .iterate(tempFile.bufferedReader())
                        .forEach { record ->
                            writeRow(record)
                        }
                }
            close()
        }
    }
}
