package tasks

import database.DatabaseConnection
import database.pipelineRunTasks
import database.sourceTables
import org.ktorm.dsl.eq
import org.ktorm.dsl.notEq
import org.ktorm.dsl.update
import org.ktorm.entity.any
import org.ktorm.entity.filter
import org.ktorm.entity.toList
import orm.entities.runFilesLocation
import orm.enums.FileCollectType
import orm.tables.PipelineRunTasks
import orm.tables.PipelineRuns
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

    override suspend fun run() {
        val pipelineRun = PipelineRuns.getRun(task.runId) ?: throw Exception("Run cannot be null")
        val folder = File(pipelineRun.runFilesLocation)
        require(folder.exists()) {
            "Files location specified by data source does not exist or the system does not have access"
        }
        require(folder.isDirectory) { "Files location specified by data source is not a directory" }
        val hasScanned = DatabaseConnection
            .database
            .pipelineRunTasks
            .filter { it.runId eq task.runId }
            .filter { it.taskId eq taskId }
            .filter { it.pipelineRunTaskId notEq pipelineRunTaskId }
            .any()
        val sourceFiles = DatabaseConnection
            .database
            .sourceTables
            .filter { it.runId eq task.runId }
            .toList()
        val files = folder
            .walk()
            .filter { it.isFile }
            .toList()
        val missingFiles = sourceFiles.filter { sourceFile -> !files.any { sourceFile.fileName == it.name } }
        val extraFiles = files.filter { file -> !sourceFiles.any { file.name == it.fileName } }
        val downloadTypes = listOf(FileCollectType.REST, FileCollectType.Download)
        if (missingFiles.isNotEmpty()) {
            if (hasScanned)
                throw IllegalStateException("Attempted to rescan after download but still missing files")
            val downloadTaskId = missingFiles
                .filter { it.collectType in downloadTypes }
                .map { it.fileName }
                .distinct()
                .let { downloadFiles ->
                    if (downloadFiles.isNotEmpty()) {
                        PipelineRunTasks.addTask(task, DownloadMissingFiles.taskId)?.also { downloadTaskId ->
                            DatabaseConnection.database.update(PipelineRunTasks) {
                                set(
                                    it.taskMessage,
                                    downloadFiles.joinToString(
                                        separator = "','",
                                        prefix = "['",
                                        postfix = "']"
                                    )
                                )
                                where { it.pipelineRunTaskId eq downloadTaskId }
                            }
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
                        PipelineRunTasks.addTask(task, CollectMissingFiles.taskId)?.also { collectTaskId ->
                            DatabaseConnection.database.update(PipelineRunTasks) {
                                set(
                                    it.taskMessage,
                                    missingFiles.joinToString(
                                        separator = "','",
                                        prefix = "['",
                                        postfix = "']"
                                    )
                                )
                                where { it.pipelineRunTaskId eq collectTaskId }
                            }
                        }
                    } else {
                        null
                    }
                }
            if (downloadTaskId != null || collectFilesTaskId != null) {
                PipelineRunTasks.addTask(task, taskId)
            }
        }
        if (extraFiles.isNotEmpty()) {
            val fileNames = extraFiles.joinToString { it.name }
            setMessage("Found extra files that could be added to source tables. $fileNames")
        }
    }
}