package tasks

import database.DatabaseConnection
import database.tables.PipelineRunTasks
import database.enums.FileCollectType
import database.tables.PipelineRuns
import web.*

/**
 * System task that downloads missing files that have a URL to reference.
 *
 * Finds all file names that were passed as a message to the task and attempts to download all the returned URLs
 */
class DownloadMissingFiles(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    companion object {
        const val taskId: Long = 4
        val downloadCollectTypes = arrayOf(FileCollectType.Download.pgObject, FileCollectType.REST.pgObject)
    }
    override val taskId: Long = 4

    override suspend fun run(task: PipelineRunTasks.PipelineRunTask) {
        requireNotNull(task.taskMessage) { "Task message must contain missing filenames" }
        val pipelineRun = PipelineRuns.getRun(task.runId) ?: throw Exception("Run cannot be null")
        val outputFolder = pipelineRun.runFilesLocation
        val filenames = task.taskMessage
            .trim('[', ']')
            .split("','")
            .map { it.trim('\'') }
            .toTypedArray()
        val sql = """
            SELECT DISTINCT url
            FROM   ${database.tables.SourceTables.tableName}
            WHERE  run_id = ?
            AND    file_name in (${"?,".repeat(filenames.size).trim(',')})
            AND    collect_type in (?,?)
            AND    url IS NOT NULL
        """.trimIndent()
        val parameters = listOf(task.runId, *filenames, *downloadCollectTypes, )
        for (url in DatabaseConnection.submitQuery<String>(sql, parameters)) {
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