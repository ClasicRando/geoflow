package tasks

import data_loader.checkTableExists
import data_loader.loadFile
import database.DatabaseConnection
import database.sourceTableColumns
import database.sourceTables
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.groupBy
import org.ktorm.entity.joinToString
import orm.entities.runFilesLocation
import orm.tables.PipelineRuns
import java.io.File

class LoadFiles(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    override val taskId: Long = 13
    override suspend fun run() {
        val pipelineRun = PipelineRuns.getRun(task.runId) ?: throw IllegalArgumentException("Run ID must not be null")
        DatabaseConnection.database.let { database ->
            database.useConnection { connection ->
                database
                    .sourceTables
                    .filter { it.runId eq  task.runId }
                    .filter { it.load }
                    .groupBy { it.fileName }
                    .forEach { (fileName, sourceTables) ->
                        val file = File(pipelineRun.runFilesLocation, fileName)
                        val (tableNames, subTables) = sourceTables.map { Pair(it.tableName, it.subTable) }.unzip()
                        val (delimiter, qualified) = sourceTables.first().let { sourceTable ->
                            sourceTable.delimiter?.first() to sourceTable.qualified
                        }
                        for (sourceTable in sourceTables) {
                            val createStatement = database
                                .sourceTableColumns
                                .filter { it.stOid eq sourceTable.stOid }
                                .joinToString(
                                    separator = " text,",
                                    prefix = "create table ${sourceTable.tableName} (",
                                    postfix = " text)"
                                ) {
                                    it.name
                                }
                            if (connection.checkTableExists(sourceTable.tableName)) {
                                logger.info("Dropping ${sourceTable.tableName} to load")
                                connection
                                    .prepareStatement("drop table ${sourceTable.tableName}")
                                    .use { statement ->
                                        statement.execute()
                                    }
                            }
                            connection.prepareStatement(createStatement).use { statement ->
                                statement.execute()
                            }
                        }
                        connection.loadFile(
                            file = file,
                            tableNames = tableNames,
                            subTableNames = subTables.filterNotNull(),
                            delimiter = delimiter ?: ',',
                            qualified = qualified
                        )
                    }
            }
        }
    }
}
