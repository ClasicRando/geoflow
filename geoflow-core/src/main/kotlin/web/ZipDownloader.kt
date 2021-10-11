package web

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
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
                with(File.createTempFile("temp", ".zip")) {
                    response.content.copyTo(this.writeChannel(Dispatchers.IO))
                    ZipFile(this).use { zip ->
                        for (entry in zip.entries()) {
                            zip.getInputStream(entry)
                                .copyTo(File(outputPath, entry.name).writeChannel(Dispatchers.IO))
                        }
                    }
                    deleteOnExit()
                }
            }
        }
    }
}