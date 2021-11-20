package tasks

import loading.AnalyzeResult
import loading.LoadingInfo
import loading.analyzeFile
import loading.checkTableExists
import loading.loadFile
import database.extensions.runBatchUpdate
import database.tables.PipelineRunTasks
import database.tables.PipelineRuns
import database.tables.SourceTableColumns
import database.tables.SourceTables
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.sql.Connection

/**
 * System task to analyze all the source files for a pipeline run that are marked to be analyzed.
 *
 * Iterates over all the source tables in a pipeline run that have a 'true' analyze field and analyze the specified
 * file. One file might have multiple sub tables so each source table record is grouped by filename. After the files
 * have been analyzed, the column stats are inserted (or updated if they already exist) into the [SourceTableColumns]
 * table and the [SourceTables] record is updated to show it has been analyzed.
 */
@SystemTask(taskId = 12)
suspend fun analyzeFiles(connection: Connection, prTask: PipelineRunTasks.PipelineRunTask) {
    val pipelineRun = PipelineRuns.getRun(connection, prTask.runId)
        ?: throw IllegalArgumentException("Run ID must not be null")
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
@SystemTask(taskId = 13)
suspend fun loadFiles(connection: Connection, prTask: PipelineRunTasks.PipelineRunTask) {
    val pipelineRun = PipelineRuns.getRun(connection, prTask.runId)
        ?: throw IllegalArgumentException("Run ID must not be null")
    val updateSql = "UPDATE ${SourceTables.tableName} SET load = false WHERE st_oid = ?"
    for (file in SourceTables.filesToLoad(connection, prTask.runId)) {
        for (loadingInfo in file.loaders) {
            createSourceTable(connection, loadingInfo)
        }
        connection.loadFile(
            file = File(pipelineRun.runFilesLocation, file.fileName),
            loaders = file.loaders,
        )
        connection.runBatchUpdate(
            sql = updateSql,
            parameters = file.loaders.map { it.stOid }
        )
    }
}

/** Utility function to create source table using the provided [loadingInfo]. Drops table if it already exists */
private fun createSourceTable(connection: Connection, loadingInfo: LoadingInfo) {
    if (connection.checkTableExists(loadingInfo.tableName)) {
        taskLogger.info("Dropping ${loadingInfo.tableName} to load")
        connection.prepareStatement("drop table ${loadingInfo.tableName}").use { statement ->
            statement.execute()
        }
    }
    connection.prepareStatement(loadingInfo.createStatement).use { statement ->
        statement.execute()
    }
}
