package tasks

import database.DatabaseConnection
import database.enums.FileCollectType
import database.tables.PipelineRunTasks
import database.tables.PipelineRuns
import database.tables.SourceTables
import java.io.File

/**
 * System task that scans the source folder to find any missing or extra files in the 'files' folder.
 *
 * Collects all source tables associated with the run and walks the 'files' folder to find existing and required files.
 * Finding all missing files that are required and separates those files as downloadable (Download or REST collect type)
 * or collectable (all other file types that require manual intervention to collect). Once the missing file requirements
 * are found, the appropriate tasks ([DownloadMissingFiles] and [CollectMissingFiles]) are added as children tasks with
 * a second [ScanSourceFolder] task to valid after the missing files are resolved.
 */
class ScanSourceFolder(pipelineTaskId: Long): SystemTask(pipelineTaskId) {

    override val taskId: Long = 3

    private val downloadTypes = listOf(FileCollectType.REST.name, FileCollectType.Download.name)

    override suspend fun run(task: PipelineRunTasks.PipelineRunTask) {
        val pipelineRun = PipelineRuns.getRun(task.runId) ?: throw Exception("Run cannot be null")
        val folder = File(pipelineRun.runFilesLocation)
        require(folder.exists()) {
            "Files location specified by data source does not exist or the system does not have access"
        }
        require(folder.isDirectory) { "Files location specified by data source is not a directory" }
        val hasScanned = DatabaseConnection.queryConnectionSingle { connection ->
            connection.prepareStatement("""
                SELECT *
                FROM   ${PipelineRunTasks.tableName}
                WHERE  run_id = ?
                AND    task_id = ?
                AND    pr_task_id != ?
                LIMIT 1
            """.trimIndent()).use { statement ->
                statement.setLong(1, task.runId)
                statement.setLong(2, taskId)
                statement.setLong(3, pipelineRunTaskId)
                statement.executeQuery().use {
                    it.next()
                }
            }
        }
        val sourceFiles = SourceTables.getRunSourceTables(task.runId)
        val files = folder
            .walk()
            .filter { it.isFile }
            .toList()
        val missingFiles = sourceFiles.filter { sourceFile -> !files.any { sourceFile.fileName == it.name } }
        val extraFiles = files.filter { file -> !sourceFiles.any { file.name == it.fileName } }
        if (missingFiles.isNotEmpty()) {
            if (hasScanned)
                error("Attempted to rescan after download but still missing files")
            val downloadTaskId = missingFiles
                .filter { it.collectType in downloadTypes }
                .map { it.fileName }
                .distinct()
                .let { downloadFiles ->
                    if (downloadFiles.isNotEmpty()) {
                        PipelineRunTasks.addTask(pipelineRunTaskId, DownloadMissingFiles.taskId)
                            ?.also { downloadTaskId ->
                                PipelineRunTasks.update(
                                    downloadTaskId,
                                    taskMessage = missingFiles.joinToString(
                                        separator = "','",
                                        prefix = "['",
                                        postfix = "']"
                                    ) {
                                        it.fileName
                                    }
                                )
                            }
                    } else {
                        null
                    }
                }
            val collectFilesTaskId = missingFiles
                .filter { it.collectType !in downloadTypes }
                .map { it.fileName }
                .distinct()
                .let { collectFiles ->
                    if (collectFiles.isNotEmpty()) {
                        PipelineRunTasks.addTask(pipelineRunTaskId, CollectMissingFiles.taskId)
                            ?.also { collectTaskId ->
                                PipelineRunTasks.update(
                                    collectTaskId,
                                    taskMessage = missingFiles.joinToString(
                                        separator = "','",
                                        prefix = "['",
                                        postfix = "']"
                                    ) {
                                        it.fileName
                                    }
                                )
                            }
                    } else {
                        null
                    }
                }
            if (downloadTaskId != null || collectFilesTaskId != null) {
                PipelineRunTasks.addTask(pipelineRunTaskId, taskId)
            }
        }
        if (extraFiles.isNotEmpty()) {
            val fileNames = extraFiles.joinToString { it.name }
            setMessage("Found extra files that could be added to source tables. $fileNames")
        }
    }
}