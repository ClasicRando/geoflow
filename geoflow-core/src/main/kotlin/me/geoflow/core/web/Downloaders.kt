package me.geoflow.core.web

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpStatement
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

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
    outputPath: File,
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
suspend fun downloadZip(url: String, outputPath: File): List<String> {
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
