package tasks

import database.DatabaseConnection
import database.pipelineRunTasks
import database.sourceTables
import formatLocalDateDefault
import org.ktorm.dsl.eq
import org.ktorm.entity.any
import org.ktorm.entity.filter
import org.ktorm.entity.toList
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
            .any()
        val pipelineRun = PipelineRuns.getRun(task.runId) ?: throw Exception("Run cannot be null")
        val sourceFiles = DatabaseConnection
            .database
            .sourceTables
            .filter { it.runId eq task.runId }
            .toList()
        val path = "${pipelineRun.dataSource.filesLocation}/${formatLocalDateDefault(pipelineRun.recordDate)}/files"
        val folder = File(path)
        if (!folder.exists())
            throw Exception("Files location specified by data source does not exist or the system does not have access")
        if (!folder.isDirectory)
            throw Exception("Files location specified by data source is not a directory")
        val files = folder
            .walk()
            .toList()
        val missingFiles = sourceFiles.filter { sourceFile -> !files.any { sourceFile.fileName == it.name } }
        val extraFiles = files.filter { file -> !sourceFiles.any { file.name == it.fileName } }
        if (missingFiles.isNotEmpty()) {
            when {
                hasScanned -> throw Exception("Attempted to rescan after download but still missing files")
                missingFiles.any { it.url.isNotEmpty() } -> PipelineRunTasks.addTask(task, DownloadMissingFiles.taskId)
                else -> throw Exception("Found missing files and non of them could be downloaded")
            }
        }
        if (extraFiles.isNotEmpty()) {
            val fileNames = extraFiles.joinToString { it.name }
            task.taskMessage = "Found extra files that could be added to source tables. $fileNames"
            task.flushChanges()
        }
    }
}