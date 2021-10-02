package tasks

import database.DatabaseConnection
import kotlinx.coroutines.*
import org.ktorm.dsl.*
import orm.entities.runFilesLocation
import orm.enums.FileCollectType
import orm.tables.PipelineRuns
import orm.tables.SourceTables
import web.ArcGisScraper
import web.FileDownloader
import web.ZipDownloader

class DownloadMissingFiles(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    companion object {
        const val taskId: Long = 4
        val downloadCollectTypes = listOf(FileCollectType.Download, FileCollectType.REST)
    }
    override val taskId: Long = 4

    override suspend fun run() {
        if (task.taskMessage == null)
            throw IllegalArgumentException("Task message must contain missing filenames")
        val pipelineRun = PipelineRuns.getRun(task.runId) ?: throw Exception("Run cannot be null")
        val outputFolder = pipelineRun.runFilesLocation
        val filenames = task.taskMessage!!
            .trim('[', ']')
            .split("','")
            .map { it.trim('\'') }
        DatabaseConnection
            .database
            .from(SourceTables)
            .selectDistinct(SourceTables.url)
            .whereWithConditions {
                it += SourceTables.runId eq task.runId
                it += SourceTables.fileName.inList(filenames)
                it += SourceTables.collectType.inList(downloadCollectTypes)
                it += SourceTables.url.isNotNull()
            }
            .mapNotNull { row -> row[SourceTables.url] }
            .forEach { url ->
                when {
                    url.contains("/arcgis/rest/", ignoreCase = true) -> {
                        ArcGisScraper.fromUrl(
                            url = url,
                            outputPath = outputFolder
                        ).scrape()
                    }
                    url.endsWith(".zip") -> {
                        ZipDownloader(
                            url = url,
                            outputPath = outputFolder
                        ).request()
                    }
                    else -> {
                        FileDownloader(
                            url = url,
                            outputPath = outputFolder
                        ).request()
                    }
                }
            }
    }
}