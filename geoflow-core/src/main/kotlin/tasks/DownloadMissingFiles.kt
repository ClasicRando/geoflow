package tasks

import database.DatabaseConnection
import org.ktorm.dsl.*
import orm.entities.runFilesLocation
import orm.enums.FileCollectType
import orm.tables.PipelineRuns
import orm.tables.SourceTables
import web.*

/**
 * System task that downloads missing files that have a URL to reference.
 *
 * Finds all file names that were passed as a message to the task and attempts to download all the returned URLs
 */
class DownloadMissingFiles(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    companion object {
        const val taskId: Long = 4
        val downloadCollectTypes = listOf(FileCollectType.Download, FileCollectType.REST)
    }
    override val taskId: Long = 4

    override suspend fun run() {
        requireNotNull(task.taskMessage) { "Task message must contain missing filenames" }
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
                        scrapeArcGisService(
                            url = url,
                            outputPath = outputFolder
                        )
                    }
                    url.endsWith(".zip") -> {
                        downloadZip(
                            url = url,
                            outputPath = outputFolder
                        )
                    }
                    else -> {
                        downloadFile(
                            url = url,
                            outputPath = outputFolder
                        )
                    }
                }
            }
    }
}