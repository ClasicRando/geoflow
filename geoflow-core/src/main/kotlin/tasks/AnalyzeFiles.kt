package tasks

import data_loader.analyzeFile
import database.DatabaseConnection
import database.sourceTables
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.groupBy
import org.ktorm.support.postgresql.insertOrUpdate
import orm.entities.runFilesLocation
import orm.tables.PipelineRuns
import orm.tables.SourceTableColumns
import java.io.File

class AnalyzeFiles(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    override val taskId: Long = 12
    override suspend fun run() {
        val pipelineRun = PipelineRuns.getRun(task.runId) ?: throw IllegalArgumentException("Run ID must not be null")
        DatabaseConnection.database.run {
            sourceTables
                .filter { it.runId eq  task.runId }
                .filter { it.analyze }
                .groupBy { it.fileName }
                .forEach { (fileName, sourceTables) ->
                    val file = File(pipelineRun.runFilesLocation, fileName)
                    val (tableNames, subTables) = sourceTables.map { Pair(it.tableName, it.subTable) }.unzip()
                    val (delimiter, qualified) = sourceTables.first().let { sourceTable ->
                        sourceTable.delimiter?.first() to sourceTable.qualified
                    }
                    analyzeFile(
                        file = file,
                        tableNames = tableNames,
                        subTableNames = subTables.filterNotNull(),
                        delimiter = delimiter ?: ',',
                        qualified = qualified
                    ).buffer().collect { analyzeResult ->
                        val sourceTable = sourceTables.first { it.tableName == analyzeResult.tableName }
                        val repeats = analyzeResult.columns
                            .groupingBy { it.name }
                            .eachCount()
                            .filter { it.value > 1 }
                            .toMutableMap()
                        analyzeResult.columns.forEach { column ->
                            val columnName = repeats[column.name]?.let { repeatCount ->
                                repeats[column.name] = repeatCount - 1
                                "${column.name}_${repeatCount}"
                            } ?: column.name
                            insertOrUpdate(SourceTableColumns) {
                                set(SourceTableColumns.name, columnName)
                                set(SourceTableColumns.type, column.type)
                                set(SourceTableColumns.maxLength, column.maxLength)
                                set(SourceTableColumns.minLength, column.minLength)
                                set(SourceTableColumns.label, "")
                                set(SourceTableColumns.stOid, sourceTable.stOid)
                                onConflict(SourceTableColumns.stOid, SourceTableColumns.name) {
                                    set(SourceTableColumns.type, column.type)
                                    set(SourceTableColumns.maxLength, column.maxLength)
                                    set(SourceTableColumns.minLength, column.minLength)
                                }
                            }
                        }
                    }
                }

        }
    }
}
