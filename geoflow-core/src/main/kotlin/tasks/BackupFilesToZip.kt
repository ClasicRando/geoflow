package tasks

import database.tables.PipelineRunTasks
import formatLocalDateDefault
import database.tables.PipelineRuns
import java.io.File
import java.sql.Connection
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * System task that backs up the source files to a zip file.
 *
 * Uses the link to the folder containing the source files for the current pipeline run and copies all files to a
 * single zip file for backup.
 */
class BackupFilesToZip(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    override val taskId: Long = 7

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun run(connection: Connection, task: PipelineRunTasks.PipelineRunTask) {
        val pipelineRun = PipelineRuns.getRun(connection, task.runId) ?: throw Exception("Run cannot be null")
        val backupDirectory = File(pipelineRun.runZipLocation)
        if (!backupDirectory.exists()) {
            backupDirectory.mkdir()
        }
        val zipName = pipelineRun.backupZip
        with(File(backupDirectory.absolutePath, zipName)) {
            if (!exists()) {
                createNewFile()
            }
            ZipOutputStream(this.outputStream()).use { zip ->
                File(pipelineRun.runFilesLocation)
                    .walk()
                    .filter { it.isFile }
                    .forEach { sourceFile ->
                        zip.putNextEntry(ZipEntry(sourceFile.name))
                        sourceFile.bufferedReader().copyTo(zip.bufferedWriter())
                        zip.closeEntry()
                    }
            }
        }
    }
}