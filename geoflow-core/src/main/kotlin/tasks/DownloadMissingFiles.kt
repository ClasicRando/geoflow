package tasks

import database.DatabaseConnection
import database.sourceTables
import org.ktorm.dsl.eq
import org.ktorm.dsl.inList
import org.ktorm.dsl.isNotNull
import org.ktorm.entity.filter
import org.ktorm.entity.forEach
import orm.tables.PipelineRuns
import web.ArcGisScraper
import web.FileDownloader

class DownloadMissingFiles(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    companion object {
        const val taskId: Long = 4
    }
    override val taskId: Long = 4

    override suspend fun run() {
        if (task.taskMessage == null)
            throw IllegalArgumentException("Task message must contain missing filenames")
        val pipelineRun = PipelineRuns.getRun(task.runId) ?: throw Exception("Run cannot be null")
        val outputFolder = pipelineRun.dataSource.filesLocation
        val filenames = task.taskMessage!!
            .trim('[', ']')
            .split(",")
            .map { it.trim('\'') }
        DatabaseConnection
            .database
            .sourceTables
            .filter { it.runId eq task.runId }
            .filter { it.fileName.inList(filenames) }
            .filter { it.url.isNotNull() }
            .forEach { sourceTable ->
                if (sourceTable.url.contains("/arcgis/rest/", ignoreCase = true)) {
                    ArcGisScraper.fromUrl(
                        url = sourceTable.url,
                        outputPath = outputFolder
                    ).scrape()
                } else {
                    FileDownloader(
                        url = sourceTable.url,
                        outputPath = outputFolder,
                        filename = sourceTable.fileName
                    ).request()
                }
            }
    }
}