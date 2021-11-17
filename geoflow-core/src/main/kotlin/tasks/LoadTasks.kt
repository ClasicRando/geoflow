package tasks

import data_loader.*
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
    val filesToLoad = SourceTables.filesToLoad(connection, prTask.runId)
    for (file in filesToLoad) {
        for (loadingInfo in file.loaders) {
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
        connection.loadFile(
            file = File(pipelineRun.runFilesLocation, file.fileName),
            loaders = file.loaders,
        )
        connection.prepareStatement(
            "UPDATE ${SourceTables.tableName} SET load = false WHERE st_oid = ?"
        ).use { statement ->
            for (loadingInfo in file.loaders) {
                statement.setLong(1, loadingInfo.stOid)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }
}
