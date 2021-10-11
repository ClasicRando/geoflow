package web

import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import com.univocity.parsers.csv.CsvWriter
import com.univocity.parsers.csv.CsvWriterSettings
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.io.IOException

class ArcGisScraper private constructor(
    private val metadata: ArcGisServiceMetadata,
    private val outputPath: String
) {

    private val csvSettings = CsvWriterSettings().apply {
        setHeaders(*metadata.fields.toTypedArray())
    }
    private val csvParserSettings = CsvParserSettings().apply {
        isHeaderExtractionEnabled = true
    }

    @Throws(IOException::class)
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun scrape() = coroutineScope {
        val file = File(outputPath, "${metadata.name}.csv")
        withContext(Dispatchers.Default) {
            file.createNewFile()
        }
        with(CsvWriter(file, csvSettings)) {
            writeHeaders()
            metadata.queries.asFlow().map { url ->
                fetchQuery(url)
            }.buffer().collect { tempFile ->
                val reader = CsvParser(csvParserSettings)
                reader.iterate(tempFile.bufferedReader()).forEach { record ->
                    writeRow(record)
                }
            }
            close()
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun handleRecord(feature: JsonObject): Map<String, Any?> {
        val record = Json.decodeFromJsonElement<Map<String, JsonPrimitive>>(
            feature["attributes"] ?: JsonObject(mapOf())
        ).mapValues { (_, value) -> value.contentOrNull }
        val geoFields = when (metadata.geoType) {
            "esriGeometryPoint" -> feature["geometry"]
                ?.jsonObject
                ?.mapValues { (_, value) -> value.jsonPrimitive.contentOrNull } ?: mapOf()
            "esriGeometryMultipoint" -> mapOf(
                "POINTS" to Json.encodeToString(feature["geometry"]?.jsonObject?.get("points")?.jsonArray).trim()
            )
            "esriGeometryPolygon" -> mapOf(
                "RINGS" to Json.encodeToString(feature["geometry"]?.jsonObject?.get("rings")?.jsonArray).trim()
            )
            else -> mapOf()
        }
        return record + geoFields
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Throws(IllegalStateException::class)
    private suspend fun fetchQuery(url: String) = coroutineScope {
        var tryNumber = 1
        var invalidResponse = true
        var jsonResponse = JsonObject(mapOf())
        HttpClient(CIO).use { client ->
            while (invalidResponse) {
                if (tryNumber > maxTries)
                    throw IllegalArgumentException("Too many tries to fetch query. $url")
                val response = client.get<HttpResponse>(url)
                if (!response.status.isSuccess()) {
                    tryNumber++
                    delay(delayMilli)
                    continue
                }
                val json = Json.decodeFromString<JsonObject>(response.readText())
                if ("features" !in json) {
                    if ("error" in json) {
                        tryNumber++
                        delay(delayMilli)
                        continue
                    } else {
                        throw IllegalStateException("Response was not an error but no features found")
                    }
                } else {
                    invalidResponse = false
                    jsonResponse = json
                }
            }
        }
        async(Dispatchers.IO) {
            File.createTempFile("temp", ".csv").also { file ->
                file.deleteOnExit()
                val headerMapping = metadata.fields.associateWith { it }
                val writer = CsvWriter(file.bufferedWriter(), csvSettings)
                writer.writeHeaders()
                jsonResponse["features"]?.jsonArray?.let { array ->
                    val records = array.map { handleRecord(it.jsonObject) }
                    val rowData = metadata.fields.associateWith { field -> records.map { it[field] }.toTypedArray() }
                    writer.writeObjectRows(headerMapping, rowData)
                }
                writer.close()
            }
        }
    }.await()

    companion object {
        suspend fun fromUrl(url: String, outputPath: String) = ArcGisScraper(
            ArcGisServiceMetadata.fromUrl(url),
            outputPath,
        )
        private const val delayMilli = 10000L
        private const val maxTries = 10
    }
}