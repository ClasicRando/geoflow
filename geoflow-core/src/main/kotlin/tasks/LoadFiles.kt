package tasks

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
import java.sql.SQLException

class LoadFiles(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    override val taskId: Long = 13
    override suspend fun run() {
        val pipelineRun = PipelineRuns.getRun(task.runId) ?: throw IllegalArgumentException("Run ID must not be null")
        DatabaseConnection.database.run {
            useConnection { connection ->
                sourceTables
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
                            val createStatement = sourceTableColumns
                                .filter { it.stOid eq sourceTable.stOid }
                                .joinToString(
                                    separator = " text,",
                                    prefix = "create table ${sourceTable.tableName} (",
                                    postfix = " text)"
                                ) {
                                    it.name
                                }
                            try {
                                connection.prepareStatement(createStatement).execute()
                            } catch (ex: SQLException) {
                                connection.prepareStatement("drop table ${sourceTable.tableName}").execute()
                                connection.prepareStatement(createStatement).execute()
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
