package web

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.serialization.json.*
import kotlin.jvm.Throws
import kotlin.math.ceil

class ArcGisServiceMetadata private constructor(
    val url: String,
    val name: String,
    val sourceCount: Int,
    val maxRecordCount: Int,
    val pagination: Boolean,
    val stats: Boolean,
    val serverType: String,
    val geoType: String,
    val fields: List<String>,
    val oidField: String,
    val maxMinOid: Pair<Int, Int>,
    val incrementalOid: Boolean
) {
    val scrapeCount = maxRecordCount.takeIf { maxRecordCount <= 10000 } ?: 10000
    val oidQueryCount = ceil((maxMinOid.first - maxMinOid.second + 1) / scrapeCount.toFloat()).toInt()
    val paginationQueryCount = ceil(sourceCount / scrapeCount.toFloat()).toInt()
    val isTable = serverType == "TABLE"
    val geoText = if (isTable) "" else "&geometryType=${geoType}&outSR=4269"
    val queries = when {
        pagination -> (0 until paginationQueryCount)
            .asSequence()
            .map { url + paginationQuery(it) }
        oidField.isNotEmpty() -> (0 until paginationQueryCount)
            .asSequence()
            .map { url + oidQuery(maxMinOid.second + (it * scrapeCount)) }
        else -> sequenceOf()
    }

    private fun paginationQuery(queryNumber: Int): String {
        return """
            /query?where=1+%3D+1&resultOffset=${queryNumber * scrapeCount}
            &resultRecordCount=${scrapeCount}${geoText}&outFields=*&f=json
        """.trimIndent()
    }

    private fun oidQuery(minOid: Int): String {
        return """
            /query?where=${oidField}+>%3D+${minOid}+and+${oidField}+<%3D+${minOid + scrapeCount - 1}
            ${geoText}&outFields=*&f=json
        """.trimIndent()
    }

    companion object {
        private const val countQuery = "/query?where=1%3D1&returnCountOnly=true&f=json"
        private const val oidQuery = "/query?where=1%3D1&returnIdsOnly=true&f=json"
        private const val fieldQuery = "?f=json"
        private fun maxMinQuery(oidField: String) = """
           /query?outStatistics=%5B%0D%0A+%7B%0D%0A++++"statisticType"%3A+"max"%2C%0D%0A++++"onStatisticField
           "%3A+"$oidField"%2C+++++%0D%0A++++"outStatisticFieldName"%3A+"MAX_VALUE"%0D%0A
           ++%7D%2C%0D%0A++%7B%0D%0A++++"statisticType"%3A+"min"%2C%0D%0A++++"onStatisticField
           "%3A+"$oidField"%2C+++++%0D%0A++++"outStatisticFieldName"%3A+"MIN_VALUE"%0D%0A
           ++%7D%0D%0A%5D&f=json'
        """.trimIndent()

        @Throws(TypeCastException::class)
        suspend fun fromUrl(url: String): ArcGisServiceMetadata {
            return HttpClient(CIO){
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
                        advancedQuery["supportsPagination"]?.jsonPrimitive?.boolean ?: false,
                        advancedQuery["supportsStatistics"]?.jsonPrimitive?.boolean ?: false
                    )
                } else {
                    Pair(
                        json["supportsPagination"]?.jsonPrimitive?.boolean ?: false,
                        json["supportsStatistics"]?.jsonPrimitive?.boolean ?: false
                    )
                }
                val geoType = json["geometryType"]?.jsonPrimitive?.content ?: ""
                val geoFields = when(geoType) {
                    "esriGeometryPoint" -> listOf("X", "Y")
                    "esriGeometryMultipoint" -> listOf("POINTS")
                    "esriGeometryPolygon" -> listOf("RINGS")
                    else -> listOf()
                }
                val fields = json["fields"]?.jsonArray
                    ?.mapNotNull { it.jsonObject } ?: listOf()
                val fieldNames = fields
                    .filter {
                        it["name"]?.jsonPrimitive?.content != "Shape"
                                && it["type"]?.jsonPrimitive?.content != "esriFieldTypeGeometry"
                    }
                    .mapNotNull { it["name"]?.jsonPrimitive?.content }
                    .plus(geoFields)
                val oidField = fields
                    .firstOrNull { it["type"]?.jsonPrimitive?.content == "esriFieldTypeOID" }
                    ?.jsonPrimitive?.content ?: ""
                val maxMinOid = when {
                    !pagination && stats && oidField.isNotEmpty() -> {
                        with(client.get<JsonObject>(url + maxMinQuery(oidField))) {
                            this["features"]
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
                    }
                    !pagination && oidField.isNotEmpty() -> {
                        with(client.get<JsonObject>(url + oidQuery)) {
                            val objectIds = this["objectIds"]
                                ?.jsonArray
                                ?.mapNotNull { it.jsonPrimitive.int } ?: listOf()
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