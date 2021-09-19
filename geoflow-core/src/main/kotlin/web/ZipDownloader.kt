package web

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile
import kotlin.jvm.Throws

class ZipDownloader(private val url: String, private val outputPath: String) {

    @Throws(IOException::class, IllegalStateException::class)
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun request() {
        HttpClient(CIO).use { client ->
            val response: HttpResponse = client.get(url)
            val hasContentLength = "Content-Length" in response.headers
            val contentType = response.headers["Content-Type"]
                ?: throw IllegalStateException("Response must have content-type")
            if (contentType != "application/zip")
                throw IllegalStateException("Content-type must be 'application/zip'")
            val bytes = if (hasContentLength) response.readBytes() else response.receive()
            with(File.createTempFile("temp", "zip", File(outputPath))) {
                writeBytes(bytes)
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