package web

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.errors.*
import java.io.File
import kotlin.jvm.Throws

class FileDownloader(private val url: String, private val outputPath: String, private val filename: String) {

    @Throws(IOException::class)
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun request() {
        HttpClient(CIO).use { client ->
            val file = File(outputPath, filename)
            file.createNewFile()
            val response: HttpResponse = client.get(url)
            val hasContentLength = "Content-Length" in response.headers
            when (response.headers["Content-Type"]) {
                "txt/csv", "text/plain", "application/json" -> file.writeText(response.readText())
                "application/vnd.oasis.opendocument.spreadsheet",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/octet-stream" -> {
                    val bytes = if (hasContentLength) response.readBytes() else response.receive()
                    file.writeBytes(bytes)
                }
                else -> throw IOException("Unknown content type for response")
            }
        }
    }
}