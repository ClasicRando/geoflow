package tasks

import database.enums.FileCollectType
import database.queryHasResult
import database.tables.PipelineRunTasks
import database.tables.PipelineRuns
import database.tables.SourceTables
import java.io.File
import java.sql.Connection

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

    override suspend fun run(connection: Connection, task: PipelineRunTasks.PipelineRunTask) {
        val pipelineRun = PipelineRuns.getRun(connection, task.runId) ?: throw Exception("Run cannot be null")
        val folder = File(pipelineRun.runFilesLocation)
        require(folder.exists()) {
            "Files location specified by data source does not exist or the system does not have access"
        }
        require(folder.isDirectory) { "Files location specified by data source is not a directory" }
        val hasScannedSql = """
            SELECT *
            FROM   ${PipelineRunTasks.tableName}
            WHERE  run_id = ?
            AND    task_id = ?
            AND    pr_task_id != ?
            LIMIT 1
        """.trimIndent()
        val hasScanned = connection.queryHasResult(sql = hasScannedSql, task.runId, taskId, pipelineRunTaskId)
        val sourceFiles = SourceTables.getRunSourceTables(connection, task.runId)
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
                        PipelineRunTasks.addTask(connection, pipelineRunTaskId, DownloadMissingFiles.taskId)
                            ?.also { downloadTaskId ->
                                PipelineRunTasks.update(
                                    connection,
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
                        PipelineRunTasks.addTask(connection, pipelineRunTaskId, CollectMissingFiles.taskId)
                            ?.also { collectTaskId ->
                                PipelineRunTasks.update(
                                    connection,
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
                PipelineRunTasks.addTask(connection, pipelineRunTaskId, taskId)
            }
        }
        if (extraFiles.isNotEmpty()) {
            val fileNames = extraFiles.joinToString { it.name }
            setMessage("Found extra files that could be added to source tables. $fileNames")
        }
    }
}