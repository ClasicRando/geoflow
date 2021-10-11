package web

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.io.use
import kotlin.jvm.Throws

class FileDownloader(private val url: String, private val outputPath: String, filename: String? = null) {

    private val filename = filename ?: url.substringAfterLast('/')

    @Throws(IOException::class)
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun request() {
        HttpClient(CIO).use { client ->
            val file = File(outputPath, filename)
            file.createNewFile()
            client.get<HttpStatement>(url).execute { response ->
                response.content.copyTo(file.writeChannel(Dispatchers.IO))
            }
        }
    }
}