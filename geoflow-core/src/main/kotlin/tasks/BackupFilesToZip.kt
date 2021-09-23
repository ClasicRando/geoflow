package tasks

import formatLocalDateDefault
import orm.tables.PipelineRuns
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Suppress("BlockingMethodInNonBlockingContext")
class BackupFilesToZip(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    override val taskId: Long = 7

    override suspend fun run() {
        val pipelineRun = PipelineRuns.getRun(task.runId) ?: throw Exception("Run cannot be null")
        val path = "${pipelineRun.dataSource.filesLocation}/${formatLocalDateDefault(pipelineRun.recordDate)}/files"
        val backupDirectory = File(
            "${pipelineRun.dataSource.filesLocation}/${formatLocalDateDefault(pipelineRun.recordDate)}/zip"
        )
        if (!backupDirectory.exists()) {
            backupDirectory.mkdir()
        }
        with(File(backupDirectory.absolutePath, "backup.zip")) {
            if (!exists()) {
                createNewFile()
            }
            ZipOutputStream(this.outputStream()).use { zip ->
                File(path).walk().forEach { sourceFile ->
                    zip.putNextEntry(ZipEntry(sourceFile.name))
                    sourceFile.bufferedReader().copyTo(zip.bufferedWriter())
                    zip.closeEntry()
                }
            }
        }
    }
}