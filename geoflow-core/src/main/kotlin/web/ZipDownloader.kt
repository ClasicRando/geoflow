package web

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile
import kotlin.io.use
import kotlin.jvm.Throws

class ZipDownloader(private val url: String, private val outputPath: String) {

    @Throws(IOException::class, IllegalStateException::class)
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun request() {
        HttpClient(CIO).use { client ->
            client.get<HttpStatement>(url).execute { response ->
                with(File.createTempFile("temp", "zip", File(outputPath))) {
                    val channel: ByteReadChannel = response.receive()
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                        while (!packet.isEmpty) {
                            val bytes = packet.readBytes()
                            appendBytes(bytes)
                        }
                    }
                    ZipFile(this).use { zip ->
                        zip.entries().asSequence().forEach { entry ->
                            zip.getInputStream(entry).use { input ->
                                File(outputPath, entry.name).outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                    delete()
                }
            }
        }
    }
}