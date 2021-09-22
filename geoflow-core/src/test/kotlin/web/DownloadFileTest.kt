package web

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadFileTest {

    private val outputPath = "${System.getProperty("user.dir")}/test-files"

    @AfterAll
    fun clearDownloadsFolder() {
        File(outputPath).walk().forEach { file ->
            file.delete()
        }
        println("Deleted All download files")
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "https://files.nc.gov/ncdeq/Waste%20Management/DWM/UST/Databases/Codevalues.xls",
        "https://www.tceq.texas.gov/assets/public/admin/data/docs/pst_fac.txt"
    ])
    fun `download file`(url: String) {
        val filename = "(?<=/)[^/]+$".toRegex().find(url)!!.value
        runBlocking {
            FileDownloader(
                url,
                outputPath,
                filename
            ).request()
            assertTrue {
                File(outputPath, filename).exists()
            }
        }
    }
    @ParameterizedTest
    @ValueSource(strings = [
        "https://files.nc.gov/ncdeq/Waste%20Management/DWM/UST/Databases/UST%20Data/RUST_Excel.zip",
        "https://files.nc.gov/ncdeq/Waste%20Management/DWM/UST/Databases/UST%20Data/NC_Tanks_Text.zip",
        "https://files.nc.gov/ncdeq/Waste+Management/DWM/UST/Databases/UST+Data/Tanks.zip"
    ])
    fun `download zip`(url: String) {
        val filename = "(?<=/)[^/]+$".toRegex().find(url)!!.value
        runBlocking {
            FileDownloader(
                url,
                outputPath,
                filename
            ).request()
            assertTrue {
                File(outputPath, filename).exists()
            }
        }
    }
}