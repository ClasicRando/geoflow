package tasks

import data_loader.AnalyzeResult
import data_loader.analyzeFile
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
class AnalyzeFiles(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    override val taskId: Long = 12
    override suspend fun run(connection: Connection, task: PipelineRunTasks.PipelineRunTask) {
        val pipelineRun = PipelineRuns.getRun(connection, task.runId)
            ?: throw IllegalArgumentException("Run ID must not be null")
        val results = mutableMapOf<Long, AnalyzeResult>()
        for (fileInfo in SourceTables.filesToAnalyze(connection, pipelineRun.runId)) {
            val file = File(pipelineRun.runFilesLocation, fileInfo.fileName)
            analyzeFile(
                file = file,
                tableNames = fileInfo.tableNames,
                subTableNames = fileInfo.subTables,
                delimiter = fileInfo.delimiter?.get(0) ?: ',',
                qualified = fileInfo.qualified,
            ).buffer().flowOn(Dispatchers.IO).collect { analyzeResult ->
                val stOid = fileInfo.stOids[fileInfo.tableNames.indexOf(analyzeResult.tableName)]
                results[stOid] = analyzeResult
            }
        }
        SourceTables.finishAnalyze(connection, results)
    }
}
