package web

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.errors.*
import java.io.File
import kotlin.io.use
import kotlin.jvm.Throws

class FileDownloader(private val url: String, private val outputPath: String, private val filename: String) {

    @Throws(IOException::class)
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun request() {
        HttpClient(CIO).use { client ->
            val file = File(outputPath, filename)
            file.createNewFile()
            client.get<HttpStatement>(url).execute { response ->
                val channel: ByteReadChannel = response.receive()
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                    while (!packet.isEmpty) {
                        val bytes = packet.readBytes()
                        file.appendBytes(bytes)
                    }
                }
            }
        }
    }
}