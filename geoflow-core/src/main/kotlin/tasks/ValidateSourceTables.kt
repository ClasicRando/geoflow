package tasks

import database.DatabaseConnection
import database.procedures.UpdateFiles
import database.sourceTables
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.forEach
import orm.enums.LoaderType

class ValidateSourceTables(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    override val taskId: Long = 8

    override suspend fun run() {
        UpdateFiles.call(task.runId)
        DatabaseConnection
            .database
            .sourceTables
            .filter { it.runId eq task.runId }
            .forEach { sourceTable->
                if (sourceTable.loaderType == LoaderType.Excel || sourceTable.loaderType == LoaderType.MDB) {
                    if (sourceTable.subTable == null)
                        throw Exception(
                            "${sourceTable.loaderType.name} file ${sourceTable.fileName} cannot have a null sub table"
                        )
                }
            }

    }
}