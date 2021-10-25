package tasks

import data_loader.analyzeFile
import database.DatabaseConnection
import database.sourceTables
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import org.ktorm.dsl.eq
import org.ktorm.dsl.update
import org.ktorm.entity.filter
import org.ktorm.entity.groupBy
import org.ktorm.support.postgresql.insertOrUpdate
import orm.entities.runFilesLocation
import orm.tables.PipelineRuns
import orm.tables.SourceTableColumns
import orm.tables.SourceTables
import java.io.File

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
                        analyzeResult.columns.forEachIndexed { i, column ->
                            val columnName = repeats[column.name]?.let { repeatCount ->
                                repeats[column.name] = repeatCount - 1
                                "${column.name}_${repeatCount}"
                            } ?: column.name
                            insertOrUpdate(SourceTableColumns) {
                                set(it.name, columnName)
                                set(it.type, column.type)
                                set(it.maxLength, column.maxLength)
                                set(it.minLength, column.minLength)
                                set(it.label, "")
                                set(it.stOid, sourceTable.stOid)
                                set(it.columnIndex, i)
                                onConflict(it.stOid, it.name) {
                                    set(it.type, column.type)
                                    set(it.maxLength, column.maxLength)
                                    set(it.minLength, column.minLength)
                                }
                            }
                        }
                        update(SourceTables) {
                            set(it.analyze, false)
                            set(it.recordCount, sourceTable.recordCount)
                            where { it.stOid eq sourceTable.stOid }
                        }
                    }
                }

        }
    }
}
