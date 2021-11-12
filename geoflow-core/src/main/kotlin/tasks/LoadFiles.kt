package tasks

import data_loader.checkTableExists
import data_loader.loadFile
import database.tables.PipelineRunTasks
import database.tables.PipelineRuns
import database.tables.SourceTables
import java.io.File
import java.sql.Connection

/**
 * System task to load all the source files for a pipeline run that are marked to be loaded.
 *
 * Iterates over all the source tables in a pipeline run that have a 'true' load field and load the specified file. One
 * file might have multiple sub tables so each source table record is grouped by filename. After the files have been
 * loaded, the [SourceTables] record is updated to show it has been loaded.
 */
class LoadFiles(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    override val taskId: Long = 13
    override suspend fun run(connection: Connection, task: PipelineRunTasks.PipelineRunTask) {
        val pipelineRun = PipelineRuns.getRun(connection, task.runId)
            ?: throw IllegalArgumentException("Run ID must not be null")
        val filesToLoad = SourceTables.filesToLoad(connection, task.runId)
        for (file in filesToLoad) {
            for ((tableName, createStatement) in file.tableNames zip file.createStatements) {
                if (connection.checkTableExists(tableName)) {
                    logger.info("Dropping $tableName to load")
                    connection.prepareStatement("drop table $tableName").use { statement ->
                        statement.execute()
                    }
                }
                connection.prepareStatement(createStatement).use { statement ->
                    statement.execute()
                }
            }
            connection.loadFile(
                file = File(pipelineRun.runFilesLocation, file.fileName),
                tableNames = file.tableNames,
                subTableNames = file.subTables,
                delimiter = file.delimiter?.get(0) ?: ',',
                qualified = file.qualified
            )
            connection.prepareStatement(
                "UPDATE ${SourceTables.tableName} SET load = false WHERE st_oid = ?"
            ).use { statement ->
                for (stOid in file.stOids) {
                    statement.setLong(1, stOid)
                    statement.addBatch()
                }
                statement.executeBatch()
            }
        }
    }
}
