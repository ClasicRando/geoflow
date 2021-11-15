package tasks

import database.enums.FileCollectType
import database.enums.LoaderType
import database.executeNoReturn
import database.procedures.UpdateFiles
import database.queryFirstOrNull
import database.queryHasResult
import database.submitQuery
import database.tables.PipelineRunTasks
import database.tables.PipelineRuns
import database.tables.SourceTables
import web.downloadFile
import web.downloadZip
import web.scrapeArcGisService
import java.io.File
import java.sql.Connection
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val downloadCollectTypes = arrayOf(FileCollectType.Download.pgObject, FileCollectType.REST.pgObject)
private val downloadCollectNames = listOf(FileCollectType.Download.name, FileCollectType.REST.name)

/**
 * User task to validate that the user has confirmed details of run instance
 */
@UserTask
const val confirmRunInstance = 1L

/**
 * User task to remind the user to collect all files noted in the task message
 */
@UserTask
const val collectMissingFiles = 5L

/**
 * User task to validate that the user has confirmed the record date of the pipeline run is correct
 */
@UserTask
const val confirmRecordDate = 6L

/**
 * User task to notify the user the source table options are invalid
 */
@UserTask
const val fixSourceTableOptions = 9L

/**
 * User task to notify the user that this data source has never been loaded before
 */
@UserTask
const val firstPipelineDetected = 10L

/**
 * System task that backs up the source files to a zip file.
 *
 * Uses the link to the folder containing the source files for the current pipeline run and copies all files to a
 * single zip file for backup.
 */
@SystemTask(taskId = 2)
fun buildPipelineRun(connection: Connection, prTask: PipelineRunTasks.PipelineRunTask) {
    val lastRun = PipelineRuns.lastRun(connection, prTask.pipelineRunTaskId)
    if (lastRun == null) {
        PipelineRunTasks.addTask(connection, prTask.pipelineRunTaskId, 2)
        PipelineRunTasks.addTask(connection, prTask.pipelineRunTaskId, getTaskIdFromFunction(::validateFirstPipeline))
    } else {
        connection.executeNoReturn(
            sql = "DELETE FROM ${SourceTables.tableName} WHERE run_id = ?",
            prTask.runId,
        )
        val insertSql = """
                INSERT INTO ${SourceTables.tableName}(run_id,table_name,file_name,loader_type,qualified,encoding,
                                                      sub_table,file_id,url,comments,collect_type,delimiter)
                SELECT ?,table_name,file_name,loader_type,qualified,encoding,sub_table,file_id,url,comments,
                       collect_type,delimiter
                FROM   ${SourceTables.tableName}
                WHERE  run_id = ?
            """.trimIndent()
        connection.executeNoReturn(
            sql = insertSql,
            prTask.runId,
            lastRun,
        )
    }
}

/**
 * System task that scans the source folder to find any missing or extra files in the 'files' folder.
 *
 * Collects all source tables associated with the run and walks the 'files' folder to find existing and required files.
 * Finding all missing files that are required and separates those files as downloadable (Download or REST collect type)
 * or collectable (all other file types that require manual intervention to collect). Once the missing file requirements
 * are found, the appropriate tasks ([downloadMissingFiles] and [collectMissingFiles]) are added as children tasks with
 * a second [scanSourceFolder] task to valid after the missing files are resolved.
 */
@SystemTask(taskId = 3)
fun scanSourceFolder(connection: Connection, prTask: PipelineRunTasks.PipelineRunTask): String? {
    val pipelineRun = PipelineRuns.getRun(connection, prTask.runId) ?: throw Exception("Run cannot be null")
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
    val hasScanned = connection.queryHasResult(
        sql = hasScannedSql,
        prTask.runId,
        prTask.task.taskId,
        prTask.pipelineRunTaskId
    )
    val sourceFiles = SourceTables.getRunSourceTables(connection, prTask.runId)
    val files = folder
        .walk()
        .filter { it.isFile }
        .toList()
    val missingFiles = sourceFiles.filter { sourceFile ->
        !files.any { sourceFile.fileName == it.name }
    }
    val extraFiles = files.filter { file ->
        !sourceFiles.any { file.name == it.fileName }
    }
    if (missingFiles.isNotEmpty()) {
        if (hasScanned)
            error("Attempted to rescan after download but still missing files")
        val downloadTaskId = missingFiles
            .filter { it.collectType in downloadCollectNames }
            .map { it.fileName }
            .distinct()
            .let { downloadFiles ->
                if (downloadFiles.isNotEmpty()) {
                    PipelineRunTasks.addTask(
                        connection,
                        prTask.pipelineRunTaskId,
                        getTaskIdFromFunction(::downloadMissingFiles)
                    )?.also { downloadTaskId ->
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
            .filter { it.collectType !in downloadCollectNames }
            .map { it.fileName }
            .distinct()
            .let { collectFiles ->
                if (collectFiles.isNotEmpty()) {
                    PipelineRunTasks.addTask(
                        connection,
                        prTask.pipelineRunTaskId,
                        collectMissingFiles
                    )?.also { collectTaskId ->
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
            PipelineRunTasks.addTask(connection, prTask.pipelineRunTaskId, prTask.task.taskId)
        }
    }
    return if (extraFiles.isNotEmpty()) {
        val fileNames = extraFiles.joinToString { it.name }
        "Found extra files that could be added to source tables. $fileNames"
    } else {
        null
    }
}

/**
 * System task that downloads missing files that have a URL to reference.
 *
 * Finds all file names that were passed as a message to the task and attempts to download all the returned URLs
 */
@SystemTask(taskId = 4)
suspend fun downloadMissingFiles(connection: Connection, prTask: PipelineRunTasks.PipelineRunTask) {
    requireNotNull(prTask.taskMessage) { "Task message must contain missing filenames" }
    val pipelineRun = PipelineRuns.getRun(connection, prTask.runId) ?: throw Exception("Run cannot be null")
    val outputFolder = pipelineRun.runFilesLocation
    val filenames = prTask.taskMessage
        .trim('[', ']')
        .split("','")
        .map { it.trim('\'') }
        .toTypedArray()
    val sql = """
            SELECT DISTINCT url
            FROM   ${SourceTables.tableName}
            WHERE  run_id = ?
            AND    file_name in (${"?,".repeat(filenames.size).trim(',')})
            AND    collect_type in (?,?)
            AND    url IS NOT NULL
        """.trimIndent()
    for (url in connection.submitQuery<String>(sql, prTask.runId, *filenames, *downloadCollectTypes)) {
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

/**
 * System task that backs up the source files to a zip file.
 *
 * Uses the link to the folder containing the source files for the current pipeline run and copies all files to a
 * single zip file for backup.
 */
@SystemTask(taskId = 7)
fun backupFilesToZip(connection: Connection, prTask: PipelineRunTasks.PipelineRunTask) {
    val pipelineRun = PipelineRuns.getRun(connection, prTask.runId) ?: throw Exception("Run cannot be null")
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

/**
 * System task to validate the source table options are correct.
 *
 * Fixes any source files that are easily repaired with the [UpdateFiles] procedure then proceeds to validate that each
 * file that needs to contain a sub table has a non-null and non-blank sub table value
 *
 * Future Changes
 * --------------
 * - add more file validation steps
 */
@SystemTask(taskId = 8)
fun validateSourceTables(connection: Connection, prTask: PipelineRunTasks.PipelineRunTask) {
    UpdateFiles.call(connection, prTask.runId)
    val sql = """
            SELECT loader_type, file_name
            FROM   ${SourceTables.tableName}
            WHERE  run_id = ?
            AND    loader_type in (?,?)
            AND    TRIM(sub_table) IS NULL
        """.trimIndent()
    val issues = connection.prepareStatement(sql).use { statement ->
        statement.setLong(1, prTask.runId)
        statement.setObject(2, LoaderType.Excel.pgObject)
        statement.setObject(3, LoaderType.MDB.pgObject)
        statement.executeQuery().use { rs ->
            generateSequence {
                if (rs.next()) {
                    rs.getString(1) to rs.getString(2)
                } else {
                    null
                }
            }.toList()
        }
    }
    if (issues.isNotEmpty()) {
        error(issues.joinToString { "${it.first} file ${it.second} cannot have a null sub table" })
    }
}

/**
 * System task to validate the pipeline run has at least 1 [SourceTables] record entry
 */
@SystemTask(taskId = 11)
fun validateFirstPipeline(connection: Connection, prTask: PipelineRunTasks.PipelineRunTask) {
    val sourceTableCount = connection.queryFirstOrNull<Long>(
        sql = "SELECT COUNT(0) FROM ${SourceTables.tableName} WHERE run_id = ?",
        prTask.runId,
    ) ?: 0
    if (sourceTableCount == 0L) {
        error("Source must have at least 1 file")
    }
}
