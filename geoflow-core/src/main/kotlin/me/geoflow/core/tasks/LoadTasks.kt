@file:Suppress("UNUSED")
package me.geoflow.core.tasks

import me.geoflow.core.loading.AnalyzeResult
import me.geoflow.core.loading.analyzeFile
import me.geoflow.core.loading.loadFile
import me.geoflow.core.database.tables.PipelineRuns
import me.geoflow.core.database.tables.SourceTableColumns
import me.geoflow.core.database.tables.SourceTables
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import me.geoflow.core.database.extensions.executeNoReturn
import me.geoflow.core.database.extensions.queryFirstOrNull
import me.geoflow.core.database.extensions.runUpdate
import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.tables.PipelineRunTasks
import me.geoflow.core.database.tables.records.PipelineRunTask
import me.geoflow.core.web.html.SubTableDetails
import me.geoflow.core.web.html.basicTable
import java.io.File
import java.sql.Connection
import java.time.Instant
import java.time.ZoneId

/**
 * User task to notify the user that the record date of the collected data is outside the threshold for relevant data
 */
@UserTask(taskName = "Recollect Data")
const val RECOLLECT_DATA: Long = 16L

/**
 * User task with a modal window showing table stats collected by the parent task
 */
@UserTask(taskName = "Table Stats (INFO)")
const val TABLE_STATS: Long = 18L

/**
 * User task when the data source has not been loaded before so no comparison is required
 */
@UserTask(taskName = "No Table Stats Comparison Required")
const val NO_COMPARISON_REQUIRED: Long = 19L

/**
 * System task to analyze all the source files for a pipeline run that are marked to be analyzed.
 *
 * Iterates over all the source tables in a pipeline run that have a 'true' analyze field and analyze the specified
 * file. One file might have multiple sub tables so each source table record is grouped by filename. After the files
 * have been analyzed, the column stats are inserted (or updated if they already exist) into the [SourceTableColumns]
 * table and the [SourceTables] record is updated to show it has been analyzed.
 */
@SystemTask(taskId = 12, taskName = "Analyze Files")
suspend fun analyzeFiles(connection: Connection, prTask: PipelineRunTask) {
    val pipelineRun = PipelineRuns.getRun(connection, prTask.runId)
    val results = mutableMapOf<Long, AnalyzeResult>()
    for (fileInfo in SourceTables.filesToAnalyze(connection, pipelineRun.runId)) {
        val file = File(pipelineRun.runFilesLocation, fileInfo.fileName)
        analyzeFile(
            file = file,
            analyzers = fileInfo.analyzeInfo,
        ).buffer().flowOn(Dispatchers.IO).collect { analyzeResult ->
            val stOid = fileInfo.analyzeInfo.first { it.tableName == analyzeResult.tableName }.stOid
            results[stOid] = analyzeResult
        }
    }
    SourceTables.finishAnalyze(connection, results)
}

/**
 * System task to load all the source files for a pipeline run that are marked to be loaded.
 *
 * Iterates over all the source tables in a pipeline run that have a 'true' load field and load the specified file. One
 * file might have multiple sub tables so each source table record is grouped by filename. After the files have been
 * loaded, the [SourceTables] record is updated to show it has been loaded.
 */
@SystemTask(taskId = 13, taskName = "Load Files")
suspend fun loadFiles(connection: Connection, prTask: PipelineRunTask) {
    val pipelineRun = PipelineRuns.getRun(connection, prTask.runId)
    for (file in SourceTables.filesToLoad(connection, prTask.runId)) {
        for (loadingInfo in file.loaders) {
            connection.executeNoReturn("DROP TABLE IF EXISTS ${loadingInfo.tableName}")
            connection.executeNoReturn(loadingInfo.createStatement)
        }
        connection.loadFile(
            file = File(pipelineRun.runFilesLocation, file.fileName),
            loaders = file.loaders,
        )
        connection.runUpdate(
            sql = """
                UPDATE ${SourceTables.tableName}
                SET    load = false
                WHERE  st_oid = (${"?,".repeat(file.loaders.size).trim(',')})
            """.trimIndent(),
            file.loaders.map { it.stOid },
        )
    }
}

/**
 * System task to look up the last run's source tables and rename the tables with the postfix of '_old'. If another
 * table is already named with '_old' that table will be dropped since those tables are from 2 runs prior.
 */
@SystemTask(taskId = 14, taskName = "Backup Old Tables")
fun backupOldTables(connection: Connection, prTask: PipelineRunTask): String {
    val lastRunId = connection.queryFirstOrNull<Long>(
        sql = """
            SELECT t1.run_id
            FROM   ${PipelineRuns.tableName} t1
            JOIN  (SELECT ds_id, run_id FROM ${PipelineRuns.tableName} WHERE run_id = ?) t2
            ON     t1.ds_id = t2.ds_id
            AND    t1.run_id != t2.run_id
            ORDER BY t1.record_date desc
            LIMIT 1
        """.trimIndent(),
        prTask.runId,
    ) ?: return "This is the first run for the data source so no need to backup previous load tables"
    val tableNames = connection.submitQuery<String>(
        sql = """
            SELECT table_name
            FROM   ${SourceTables.tableName}
            WHERE  run_id = ?
        """.trimIndent(),
        lastRunId
    )
    for (table in tableNames) {
        connection.executeNoReturn("DROP TABLE IF EXISTS ${table}_old")
        connection.executeNoReturn("ALTER TABLE IF EXISTS $table RENAME TO ${table}_old")
    }
    return "Backed up: ${tableNames.joinToString(separator = "','", prefix = "'", postfix = "'")}"
}

/** Threshold for how many days old a record date can be before the data is deemed to be old */
private const val DAYS_OLD = 14L

/**
 * System task to check if the record date of the collected data is outside the threshold for relevant data. Spawns
 * User tasks to alert the user that the data is old.
 */
@SystemTask(taskId = 15, taskName = "Check If Data Is Old")
fun checkIfDataIsOld(connection: Connection, prTask: PipelineRunTask) {
    val pipelineRun = PipelineRuns.getRun(connection, prTask.runId)
    val currentDay = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
    if (pipelineRun.recordLocalDate.plusDays(DAYS_OLD).isBefore(currentDay)) {
        PipelineRunTasks.addTask(connection, prTask.pipelineRunTaskId, RECOLLECT_DATA)
    }
}

/**
 * System task to check if the record date of the collected data is outside the threshold for relevant data. Spawns
 * User tasks to alert the user that the data is old.
 */
@SystemTask(taskId = 17, taskName = "Check Table Stats")
fun checkTableStats(connection: Connection, prTask: PipelineRunTask) {
    val lastRun = PipelineRuns.lastRun(connection, prTask.pipelineRunTaskId)
    if (lastRun == null) {
        PipelineRunTasks.addTask(connection, prTask.pipelineRunTaskId, NO_COMPARISON_REQUIRED)
        return
    }
    PipelineRunTasks.addTask(connection, prTask.pipelineRunTaskId, TABLE_STATS) {
        basicTable(
            tableId = "tableCounts",
            fields = SourceTables.tableCountComparisonFields,
            dataUrl = "/source-tables/comparisons/${prTask.runId}",
            dataField = "payload",
            subTableDetails = SubTableDetails(
                url = "/source-table-columns/comparison/{id}",
                idField = "st_oid",
                fields = SourceTableColumns.columnComparisonFields,
            ),
            clickableRows = false,
        )
    }
}
