package web

import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * Convenience method to wrap ArcGIS REST service scraping. Uses a [url] to collect [metadata][ArcGisServiceMetadata],
 * fetch scraping queries, and consolidate temp files into a single file written to the [outputPath] specified
 */
suspend fun scrapeArcGisService(url: String, outputPath: String) {
    with(ArcGisServiceMetadata.fromUrl(url)) {
        val file = File(outputPath, "$name.csv")
        with(CsvWriter(file, csvSettings)) {
            writeHeaders()
            fetchQueries()
                .buffer()
                .flowOn(Dispatchers.IO)
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
