package tasks

import database.DatabaseConnection
import database.procedures.UpdateFiles
import database.sourceTables
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.forEach
import orm.enums.LoaderType

/**
 * System task to validate the source table options are correct.
 *
 * Fixes any source files that are easily repaired with the [UpdateFiles] procedure then proceeds to validate that each
 * file that needs to contain a sub table has a non-null and non-blank sub table value
 *
 * Future Changes
 * --------------
 * - add more file validation steps
 */
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
                    if (sourceTable.subTable.isNullOrBlank())
                        throw Exception(
                            "${sourceTable.loaderType.name} file ${sourceTable.fileName} cannot have a null sub table"
                        )
                }
            }

    }
}