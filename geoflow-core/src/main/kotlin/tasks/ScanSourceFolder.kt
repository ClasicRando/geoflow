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

class ScanSourceFolder(pipelineTaskId: Long): SystemTask(pipelineTaskId) {

    override val taskId: Long = 3

    override suspend fun run() {
        val hasScanned = DatabaseConnection
            .database
            .pipelineRunTasks
            .filter { it.runId eq task.runId }
            .filter { it.taskId eq taskId }
            .filter { it.pipelineRunTaskId notEq pipelineRunTaskId }
            .any()
        val pipelineRun = PipelineRuns.getRun(task.runId) ?: throw Exception("Run cannot be null")
        val sourceFiles = DatabaseConnection
            .database
            .sourceTables
            .filter { it.runId eq task.runId }
            .toList()
        val folder = File(pipelineRun.runFilesLocation)
        if (!folder.exists())
            throw Exception("Files location specified by data source does not exist or the system does not have access")
        if (!folder.isDirectory)
            throw Exception("Files location specified by data source is not a directory")
        val files = folder
            .walk()
            .filter { it.isFile }
            .toList()
        val missingFiles = sourceFiles.filter { sourceFile -> !files.any { sourceFile.fileName == it.name } }
        val extraFiles = files.filter { file -> !sourceFiles.any { file.name == it.fileName } }
        val downloadTypes = listOf(FileCollectType.REST, FileCollectType.Download)
        if (missingFiles.isNotEmpty()) {
            if (hasScanned)
                throw Exception("Attempted to rescan after download but still missing files")
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
            setMessage {
                val fileNames = extraFiles.joinToString { it.name }
                "Found extra files that could be added to source tables. $fileNames"
            }
        }
    }
}