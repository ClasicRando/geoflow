package tasks

import database.DatabaseConnection
import org.ktorm.dsl.*
import orm.tables.PipelineRunTasks
import orm.tables.PipelineRuns
import orm.tables.SourceTables

class BuildPipelineRun(pipelineRunTaskId: Long): SystemTask(pipelineRunTaskId) {

    override val taskId: Long = 1

    override suspend fun run() {
        val lastRun = PipelineRuns.lastRun(task)
        if (lastRun == null) {
            with(PipelineRunTasks) {
                addTask(task, FirstPipelineDetected.taskId)
                addTask(task, ValidateFirstPipeline.taskId)
            }
        } else {
            DatabaseConnection.database.run {
                delete(SourceTables) { it.runId eq task.runId }
                from(SourceTables)
                    .select()
                    .where(SourceTables.runId eq lastRun)
                    .insertTo(SourceTables)
                update(SourceTables) {
                    set(SourceTables.analyze, true)
                    set(SourceTables.load, true)
                    where { it.runId eq task.runId }
                }
            }
        }
    }

}