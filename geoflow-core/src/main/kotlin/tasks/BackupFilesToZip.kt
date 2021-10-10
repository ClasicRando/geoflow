package tasks

import formatLocalDateDefault
import orm.entities.runFilesLocation
import orm.entities.runZipLocation
import orm.tables.PipelineRuns
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupFilesToZip(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    override val taskId: Long = 7

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun run() {
        val pipelineRun = PipelineRuns.getRun(task.runId) ?: throw Exception("Run cannot be null")
        val backupDirectory = File(pipelineRun.runZipLocation)
        if (!backupDirectory.exists()) {
            backupDirectory.mkdir()
        }
        val zipName = "${pipelineRun.dataSource.code}_${formatLocalDateDefault(pipelineRun.recordDate)}.zip"
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