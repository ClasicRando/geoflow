package web

import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import com.univocity.parsers.csv.CsvWriter
import com.univocity.parsers.csv.CsvWriterSettings
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.io.IOException
import kotlin.jvm.Throws

class ArcGisScraper private constructor(
    private val metadata: ArcGisServiceMetadata,
    private val outputPath: String
) {

    private val csvSettings = CsvWriterSettings().apply {
        setHeaders(*metadata.fields.toTypedArray())
    }

    @Throws(IOException::class)
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun scrape() {
        HttpClient(CIO).use { client ->
            val file = File(outputPath, "${metadata.name}.csv")
            file.createNewFile()
            with(CsvWriter(file, csvSettings)) {
                writeHeaders()
                metadata.queries.asFlow().map { query ->
                    fetchQuery(client, query)
                }.collect { tempFile ->
                    val reader = CsvParser(CsvParserSettings())
                    reader.iterate(tempFile.bufferedReader()).forEach { record ->
                        writeRow(record)
                    }
                }
                close()
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun handleRecord(feature: JsonObject): Map<String, Any?> {
        val record = Json.decodeFromJsonElement<Map<String, Any?>>(feature)
        val geoFields = when (metadata.geoType) {
            "esriGeometryPoint" -> feature["geometry"]
                ?.jsonObject
                ?.mapValues { (_, value) -> value.jsonPrimitive.contentOrNull } ?: mapOf()
            "esriGeometryMultipoint" -> mapOf(
                "" to Json.encodeToString(feature["geometry"]?.jsonObject?.get("points")?.jsonArray).trim()
            )
            "esriGeometryPolygon" -> mapOf(
                "" to Json.encodeToString(feature["geometry"]?.jsonObject?.get("rings")?.jsonArray).trim()
            )
            else -> mapOf()
        }
        return record + geoFields
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Throws(IllegalStateException::class)
    private suspend fun fetchQuery(client: HttpClient, url: String): File {
        var tryNumber = 1
        var invalidResponse = true
        var jsonResponse = JsonObject(mapOf())
        while (invalidResponse) {
            if (tryNumber > maxTries)
                throw IllegalArgumentException("Too many tries to fetch query. $url")
            val response = client.get<HttpResponse>(url)
            if (!response.status.isSuccess()) {
                tryNumber++
                delay(delayMilli)
                continue
            }
            val json = response.receive<JsonObject>()
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
        return File.createTempFile("temp", ".csv").also { file ->
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

    companion object {
        suspend fun fromUrl(url: String, outputPath: String) = ArcGisScraper(
            ArcGisServiceMetadata.fromUrl(url),
            outputPath,
        )
        private const val delayMilli = 10000L
        private const val maxTries = 10
    }
}