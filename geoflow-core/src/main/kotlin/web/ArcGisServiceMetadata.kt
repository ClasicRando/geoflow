package web

import com.univocity.parsers.csv.CsvParserSettings
import com.univocity.parsers.csv.CsvWriter
import com.univocity.parsers.csv.CsvWriterSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import web.ArcGisServiceMetadata.Companion.fromUrl
import java.io.File
import kotlin.math.ceil

/**
 * Class to store and work with metadata related to ArcGIS REST services. Main usage is to scrape all features but that
 * is not required and can just be used to collect or report service metadata.
 *
 * Instances are created using a companion object suspend function ([fromUrl]) since instantiation requires an HTTP
 * request. Scraping should be done using the scrapeArcGisService function but if you need access to the queries used to
 * fetch the service's data, you can use the [queries] property.
 */
@Suppress("LongParameterList")
class ArcGisServiceMetadata private constructor(
    /** base url of the REST service */
    val url: String,
    /** name of the service */
    val name: String,
    /** number of features within the service */
    val sourceCount: Int,
    /** max number of records the service allows to be scraped at once */
    val maxRecordCount: Int,
    /** flag denoting if the service supports pagination */
    val pagination: Boolean,
    /** flag denoting if the service supports statistics */
    val stats: Boolean,
    /** name of the server type */
    val serverType: String,
    /** name of the geometry type */
    val geoType: String,
    /** list of field names */
    val fields: List<String>,
    /** name of the oid field. If no field type matches OID then the field is empty */
    val oidField: String,
    /** Max and min OID values if the OID field exists and the values are required for scraping */
    val maxMinOid: Pair<Int, Int>,
    /** flag denoting if the OID field exists and is incremental */
    val incrementalOid: Boolean,
) {
    /** Scrape chunk size. Uses service max record count but caps that value to 10000 */
    private val scrapeCount = maxRecordCount.takeIf { maxRecordCount <= maxRecordScrape } ?: maxRecordScrape
    /** Number of queries required to scrape all features using the OID field */
    private val oidQueryCount = ceil((maxMinOid.first - maxMinOid.second + 1) / scrapeCount.toFloat()).toInt()
    /** Number of queries required to scape all features using pagination */
    private val paginationQueryCount = ceil(sourceCount / scrapeCount.toFloat()).toInt()
    /** True if service is a table. No geometry type is specified in query url */
    private val isTable = serverType == "TABLE"
    /** Geometry specification portion of query url */
    private val geoText = if (isTable) "" else "&geometryType=$geoType&outSR=4269"
    private val logger = KotlinLogging.logger {}
    /** Settings used to write temp csv files for each query. Specifies what headers are used */
    @Suppress("SpreadOperator")
    val csvSettings: CsvWriterSettings = CsvWriterSettings().apply {
        setHeaders(*fields.toTypedArray())
    }
    /** Settings used to parse temp csv files for each query. Specifies that the csv files have a header */
    val csvParserSettings: CsvParserSettings = CsvParserSettings().apply {
        isHeaderExtractionEnabled = true
    }
    /**
     * Sequence of query url strings for each scraping method. Returns an empty sequence when no method available
     */
    private val queries: Sequence<String> = when {
        pagination -> (0 until paginationQueryCount)
            .asSequence()
            .map { url + paginationQuery(it) }
        oidField.isNotEmpty() -> (0 until oidQueryCount)
            .asSequence()
            .map { url + oidQuery(maxMinOid.second + (it * scrapeCount)) }
        else -> sequenceOf()
    }

    /** Constructs a query url using a pagination offset of the [queryNumber] multiplied by the features per query */
    private fun paginationQuery(queryNumber: Int): String {
        return """
            /query?where=1+%3D+1&resultOffset=${queryNumber * scrapeCount}
            &resultRecordCount=${scrapeCount}$geoText&outFields=*&f=json
        """.trimIndent().replace("\n", "")
    }

    /**
     * Constructs a query url using a range of OID values. Starts at [minOid] with an upper bound defined by the
     * [scrapeCount]
     */
    private fun oidQuery(minOid: Int): String {
        return """
            /query?where=$oidField+>%3D+$minOid+and+$oidField+<%3D+${minOid + scrapeCount - 1}
            $geoText&outFields=*&f=json
        """.trimIndent().replace("\n", "")
    }

    /** Cold flow of query responses mapped to temp csv files */
    fun fetchQueries(): Flow<File> = queries.asFlow().map { query ->
        logger.info("Fetching query: $query")
        fetchQuery(query)
    }

    /**
     * Transforms a service [feature] JSON object into a [Map] of nullable Strings that can be used to write a CSV file
     */
    @OptIn(ExperimentalSerializationApi::class)
    private fun handleRecord(feature: JsonObject): Map<String, String?> {
        val record = Json.decodeFromJsonElement<Map<String, JsonPrimitive>>(
            feature["attributes"] ?: JsonObject(mapOf())
        ).mapValues { (_, value) -> value.contentOrNull }
        val geoFields = when (geoType) {
            "esriGeometryPoint" -> {
                feature["geometry"]?.jsonObject
                    ?.entries
                    ?.associate { (key, value) -> key.uppercase() to value.jsonPrimitive.contentOrNull }
                    ?: mapOf()
            }
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

    /**
     * Tries to fetch the [url] query response and returns the response if successful within the [maxTries]
     *
     * @throws IllegalStateException when [maxTries] is exceeded or a response was returned with an error key
     */
    private suspend fun HttpClient.tryUntilSuccess(url: String): JsonObject {
        var tryNumber = 1
        var invalidResponse = true
        var jsonResponse = JsonObject(mapOf())
        while (invalidResponse) {
            if (tryNumber > maxTries) {
                error("Too many tries to fetch query. $url")
            }
            val json: JsonObject = get(url)
            if ("features" !in json) {
                if ("error" in json) {
                    tryNumber++
                    delay(delayMilli)
                    continue
                } else {
                    error("Response was not an error but no features found")
                }
            } else {
                invalidResponse = false
                jsonResponse = json
            }
        }
        return jsonResponse
    }

    /**
     * Uses [url] query to fetch a JSON response and transform each feature to a writeable csv record. Returns a
     * reference to a temp file that can be used to build the entire services features.
     *
     * Makes an HTTP request and retries until it gets a valid JSON response, or it exceeds the max number of tries.
     * Once a valid response is received, a temp file is created and the features are written to the temp file.
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun fetchQuery(url: String): File {
        val jsonResponse = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }.use { client ->
            client.tryUntilSuccess(url)
        }
        return File.createTempFile("temp", ".csv").also { file ->
            file.deleteOnExit()
            val writer = CsvWriter(file.bufferedWriter(), csvSettings)
            writer.writeHeaders()
            jsonResponse["features"]?.jsonArray?.let { array ->
                for (record in array) {
                    writer.writeRow(handleRecord(record.jsonObject))
                }
            }
            writer.close()
        }
    }

    companion object {
        private const val maxRecordScrape = 10000
        private const val countQuery = "/query?where=1%3D1&returnCountOnly=true&f=json"
        private const val oidQuery = "/query?where=1%3D1&returnIdsOnly=true&f=json"
        private const val fieldQuery = "?f=json"
        /** Delay between query attempts if an erroneous response is received */
        private const val delayMilli = 10000L
        private const val maxTries = 10
        /** Query extension to find the max and min OID values for a service. Only used when stats are supported */
        private fun maxMinQuery(oidField: String) = """
           /query?outStatistics=%5B%0D%0A+%7B%0D%0A++++"statisticType"%3A+"max"%2C%0D%0A++++"onStatisticField
           "%3A+"$oidField"%2C+++++%0D%0A++++"outStatisticFieldName"%3A+"MAX_VALUE"%0D%0A
           ++%7D%2C%0D%0A++%7B%0D%0A++++"statisticType"%3A+"min"%2C%0D%0A++++"onStatisticField
           "%3A+"$oidField"%2C+++++%0D%0A++++"outStatisticFieldName"%3A+"MIN_VALUE"%0D%0A
           ++%7D%0D%0A%5D&f=json'
        """.trimIndent().replace("\n", "")

        /**
         * Returns [metadata][ArcGisServiceMetadata] from the provided [url] that points to an ArcGIS REST service.
         *
         * Only method to obtain a class instance. Since the class requires data obtained from an HTTP request, the
         * class creation method has to be suspendable to avoid the class constructor blocking the current thread.
         */
        @Suppress("LongMethod", "ComplexMethod")
        suspend fun fromUrl(url: String): ArcGisServiceMetadata {
            return HttpClient(CIO) {
                install(JsonFeature) {
                    serializer = KotlinxSerializer()
                }
            }.use { client ->
                val sourceCount = client.get<JsonObject>(url + countQuery)["count"]
                    ?.jsonPrimitive
                    ?.int ?: -1
                val json = client.get<JsonObject>(url + fieldQuery)
                val serverType = json["type"]?.jsonPrimitive?.content ?: ""
                val name = json["name"]?.jsonPrimitive?.content ?: ""
                val maxRecordCount = json["maxRecordCount"]?.jsonPrimitive?.int ?: -1
                val advancedQuery = json["advancedQueryCapabilities"]?.jsonObject
                val (pagination, stats) = if (advancedQuery != null) {
                    Pair(
                        advancedQuery["supportsPagination"]?.jsonPrimitive?.boolean == true,
                        advancedQuery["supportsStatistics"]?.jsonPrimitive?.boolean ?: false
                    )
                } else {
                    Pair(
                        json["supportsPagination"]?.jsonPrimitive?.boolean ?: false,
                        json["supportsStatistics"]?.jsonPrimitive?.boolean ?: false
                    )
                }
                val geoType = json["geometryType"]?.jsonPrimitive?.content ?: ""
                val geoFields = when (geoType) {
                    "esriGeometryPoint" -> listOf("X", "Y")
                    "esriGeometryMultipoint" -> listOf("POINTS")
                    "esriGeometryPolygon" -> listOf("RINGS")
                    else -> emptyList()
                }
                val fields = json["fields"]?.jsonArray
                    ?.mapNotNull { it.jsonObject } ?: emptyList()
                val fieldNames = fields
                    .filter {
                        it["name"]?.jsonPrimitive?.content != "Shape" &&
                                it["type"]?.jsonPrimitive?.content != "esriFieldTypeGeometry"
                    }
                    .mapNotNull { it["name"]?.jsonPrimitive?.content }
                    .plus(geoFields)
                val oidField = fields
                    .firstOrNull { it["type"]?.jsonPrimitive?.content == "esriFieldTypeOID" }
                    ?.get("name")
                    ?.jsonPrimitive?.content ?: ""
                val maxMinOid = when {
                    !pagination && stats && oidField.isNotEmpty() -> {
                        val response = client.get<JsonObject>(url + maxMinQuery(oidField))
                        response["features"]
                            ?.jsonArray
                            ?.get(0)
                            ?.jsonObject
                            ?.get("attributes")
                            ?.jsonObject
                            ?.let { attributes ->
                                Pair(
                                    attributes["MAX_VALUE"]?.jsonPrimitive?.int ?: -1,
                                    attributes["MIN_VALUE"]?.jsonPrimitive?.int ?: -1
                                )
                            } ?: Pair(-1, -1)
                    }
                    !pagination && oidField.isNotEmpty() -> {
                        with(client.get<JsonObject>(url + oidQuery)) {
                            val objectIds = this["objectIds"]
                                ?.jsonArray
                                ?.mapNotNull { it.jsonPrimitive.int } ?: emptyList()
                            Pair(objectIds.maxOrNull() ?: -1, objectIds.minOrNull() ?: -1)
                        }
                    }
                    else -> Pair(-1, -1)
                }
                val incrementalOid = maxMinOid.first != -1 && (maxMinOid.first - maxMinOid.second + 1) == sourceCount
                ArcGisServiceMetadata(
                    url = url,
                    name = name,
                    sourceCount = sourceCount,
                    maxRecordCount = maxRecordCount,
                    pagination = pagination,
                    stats = stats,
                    serverType = serverType,
                    geoType = geoType,
                    fields = fieldNames,
                    oidField = oidField,
                    maxMinOid = maxMinOid,
                    incrementalOid = incrementalOid,
                )
            }
        }
    }
}
