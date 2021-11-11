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
import kotlin.jvm.Throws

/**
 * Downloads a file from the specified [url] and writes that file to the [outputPath].
 *
 * If the file already exits, it will be overwritten by incoming file.
 *
 * @throws IOException if an I/O error has occurred
 */
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun downloadFile(
    url: String,
    outputPath: String,
    filename: String = url.substringAfterLast('/')
) {
    HttpClient(CIO).use { client ->
        val file = File(outputPath, filename)
        file.createNewFile()
        client.get<HttpStatement>(url).execute { response ->
            response.content.copyTo(file.writeChannel(Dispatchers.IO))
        }
    }
}

/**
 * Downloads a zip file from the specified [url] and unzips all the files within the zip folder to the [outputPath].
 *
 * Writes the zip download to a temp file and writes each zip entry into a file
 *
 * @throws IOException if an I/O error has occurred
 */
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun downloadZip(url: String, outputPath: String): List<String> {
    val resultFiles = mutableListOf<String>()
    HttpClient(CIO).use { client ->
        client.get<HttpStatement>(url).execute { response ->
            with(File.createTempFile("temp", ".zip")) {
                response.content.copyTo(this.outputStream())
                ZipFile(this).use { zip ->
                    for (entry in zip.entries()) {
                        zip.getInputStream(entry)
                            .copyTo(File(outputPath, entry.name).writeChannel(Dispatchers.IO))
                        resultFiles += entry.name
                    }
                }
                deleteOnExit()
            }
        }
    }
    return resultFiles
}
