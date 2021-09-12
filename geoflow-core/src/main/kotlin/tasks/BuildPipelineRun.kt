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
                addTask(task.runId, FirstPipelineDetected.taskId)
                addTask(task.runId, ValidateFirstPipeline.taskId)
            }
        } else {
            DatabaseConnection.database.run {
                delete(SourceTables) { it.run eq task.runId }
                from(SourceTables)
                    .select()
                    .where(SourceTables.run eq lastRun)
                    .insertTo(SourceTables)
                update(SourceTables) {
                    set(SourceTables.analyze, true)
                    set(SourceTables.load, true)
                    where { it.run eq task.runId }
                }
            }
        }
    }

}