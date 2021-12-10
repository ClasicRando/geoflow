@file:Suppress("UNUSED")
package me.geoflow.core.tasks

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.geoflow.core.database.enums.FileCollectType
import me.geoflow.core.database.enums.LoaderType
import me.geoflow.core.database.extensions.executeNoReturn
import me.geoflow.core.database.procedures.UpdateFiles
import me.geoflow.core.database.extensions.queryFirstOrNull
import me.geoflow.core.database.extensions.queryHasResult
import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.tables.PipelineRunTasks
import me.geoflow.core.database.tables.PipelineRuns
import me.geoflow.core.database.tables.SourceTables
import me.geoflow.core.database.tables.records.PipelineRunTask
import me.geoflow.core.web.downloadFile
import me.geoflow.core.web.downloadZip
import me.geoflow.core.web.scrapeArcGisService
import java.io.File
import java.sql.Connection
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val downloadCollectTypes = arrayOf(FileCollectType.Download.pgObject, FileCollectType.REST.pgObject)
private val downloadCollectNames = listOf(FileCollectType.Download.name, FileCollectType.REST.name)

/**
 * User task to validate that the user has confirmed details of run instance
 */
@UserTask(taskName = "Confirm Run Instance")
const val CONFIRM_RUN_INSTANCE: Long = 1L

/**
 * User task to remind the user to collect all files noted in the task message
 */
@UserTask(taskName = "Collect Missing Files")
const val COLLECT_MISSING_FILES: Long = 5L

/**
 * User task to validate that the user has confirmed the record date of the pipeline run is correct
 */
@UserTask(taskName = "Confirm Record Date")
const val CONFIRM_RECORD_DATE: Long = 6L

/**
 * User task to notify the user the source table options are invalid
 */
@UserTask(taskName = "Fix Source Table Options")
const val FIX_SOURCE_TABLE_OPTIONS: Long = 9L

/**
 * User task to notify the user that this data source has never been loaded before
 */
@UserTask(taskName = "First Pipeline Detected")
const val FIRST_PIPELINE_DETECTED: Long = 10L

/**
 * System task that backs up the source files to a zip file.
 *
 * Uses the link to the folder containing the source files for the current pipeline run and copies all files to a
 * single zip file for backup.
 */
@SystemTask(taskId = 2, taskName = "Build Pipeline Run")
fun buildPipelineRun(connection: Connection, prTask: PipelineRunTask) {
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
 * Returns a [Pair] of extra files (as [List] of [File]s) and missing filenames (as a [Map] of filename to collection
 * type).
 *
 * Uses the [runId] to get the required files and the [folder] to get the provided files. Finds missing files and extra
 * files by comparing the 2 collections and finding mutually exclusive entries on both sides.
 */
private fun checkSourceFolder(
    connection: Connection,
    runId: Long,
    folder: File,
): Pair<List<File>, Map<String, String>> {
    val fileNames = buildMap<String, Pair<String?, File?>> {
        for (sourceFile in SourceTables.getRunSourceTables(connection, runId)) {
            this[sourceFile.fileName] = Pair(sourceFile.collectType, null)
        }
        for (file in folder.walk().filter { it.isFile }) {
            this[file.name] = if (file.name in this) {
                this[file.name]!!.copy(second = file)
            } else {
                Pair(null, file)
            }
        }
    }
    val extra = fileNames
        .filter { (_, pair) -> pair.first == null && pair.second != null }
        .map { it.value.second!! }
    val missing = fileNames
        .filter { (_, pair) -> pair.first != null && pair.second == null }
        .mapValues { it.value.first!! }
    return extra to missing
}

/**
 * System task that scans the source folder to find any missing or extra files in the 'files' folder.
 *
 * Collects all source tables associated with the run and walks the 'files' folder to find existing and required files.
 * Finding all missing files that are required and separates those files as downloadable (Download or REST collect type)
 * or collectable (all other file types that require manual intervention to collect). Once the missing file requirements
 * are found, the appropriate tasks ([downloadMissingFiles] and [COLLECT_MISSING_FILES]) are added as children tasks
 * with a second [scanSourceFolder] task to valid after the missing files are resolved.
 */
@Suppress("LongMethod")
@SystemTask(taskId = 3, taskName = "Scan Source Folder")
fun scanSourceFolder(connection: Connection, prTask: PipelineRunTask): String? {
    val pipelineRun = PipelineRuns.getRun(connection, prTask.runId)
        ?: throw IllegalArgumentException("Run cannot be null")
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
    val (extraFiles, missingFiles) = checkSourceFolder(connection, prTask.runId, folder)
    if (missingFiles.isNotEmpty()) {
        if (hasScanned) {
            error("Attempted to rescan after download but still missing files")
        }
        val downloadFiles = missingFiles
            .filter { it.value in downloadCollectNames }
            .keys
        val downloadTaskId = if (downloadFiles.isNotEmpty()) {
            PipelineRunTasks.addTask(
                connection,
                prTask.pipelineRunTaskId,
                getTaskIdFromFunction(::downloadMissingFiles)
            )
        } else {
            null
        }
        val collectFiles = missingFiles
            .filter { it.value !in downloadCollectNames }
            .keys
        val collectFilesTaskId = if (collectFiles.isNotEmpty()) {
            PipelineRunTasks.addTask(
                connection,
                prTask.pipelineRunTaskId,
                COLLECT_MISSING_FILES
            )
        } else {
            null
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
@SystemTask(taskId = 4, taskName = "Download Missing Files")
suspend fun downloadMissingFiles(connection: Connection, prTask: PipelineRunTask) {
    val pipelineRun = PipelineRuns.getRun(connection, prTask.runId)
        ?: throw IllegalArgumentException("Run cannot be null")
    val outputFolder = File(pipelineRun.runFilesLocation)
    require(outputFolder.exists()) {
        "Files location specified by data source does not exist or the system does not have access"
    }
    require(outputFolder.isDirectory) { "Files location specified by data source is not a directory" }
    val (_, missingFiles) = checkSourceFolder(connection, prTask.runId, outputFolder)
    val filenames = missingFiles.keys.toTypedArray()
    val sql = """
            SELECT DISTINCT url
            FROM   ${SourceTables.tableName}
            WHERE  run_id = ?
            AND    file_name in (${"?,".repeat(filenames.size).trim(',')})
            AND    collect_type in (?,?)
            AND    url IS NOT NULL
        """.trimIndent()
    val urls = connection.submitQuery<String>(
        sql = sql,
        prTask.runId,
        filenames,
        downloadCollectTypes
    )
    for (url in urls) {
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
@SystemTask(taskId = 7, taskName = "Backup Files to Zip Folder")
suspend fun backupFilesToZip(connection: Connection, prTask: PipelineRunTask) {
    val pipelineRun = PipelineRuns.getRun(connection, prTask.runId)
        ?: throw IllegalArgumentException("Run cannot be null")
    val backupDirectory = File(pipelineRun.runZipLocation)
    if (!backupDirectory.exists()) {
        backupDirectory.mkdir()
    }
    val zipName = pipelineRun.backupZip
    with(File(backupDirectory.absolutePath, zipName)) {
        withContext(Dispatchers.IO) {
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
@Suppress("MagicNumber")
@SystemTask(taskId = 8, taskName = "Validate Source Tables")
fun validateSourceTables(connection: Connection, prTask: PipelineRunTask) {
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
            buildList {
                while (rs.next()) {
                    add(rs.getString(1) to rs.getString(2))
                }
            }
        }
    }
    if (issues.isNotEmpty()) {
        error(issues.joinToString { "${it.first} file ${it.second} cannot have a null sub table" })
    }
}

/**
 * System task to validate the pipeline run has at least 1 [SourceTables] record entry
 */
@SystemTask(taskId = 11, taskName = "Validate First Pipeline")
fun validateFirstPipeline(connection: Connection, prTask: PipelineRunTask) {
    val sourceTableCount = connection.queryFirstOrNull<Long>(
        sql = "SELECT COUNT(0) FROM ${SourceTables.tableName} WHERE run_id = ?",
        prTask.runId,
    ) ?: 0
    if (sourceTableCount == 0L) {
        error("Source must have at least 1 file")
    }
}
