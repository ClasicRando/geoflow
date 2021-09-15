package html

import orm.tables.PipelineRunTasks

class PipelineTasks(runId: Long): BasePage() {
    init {
        setContent {
            basicTable(
                "Tasks",
                "tasks",
                "/api/pipeline-run-tasks?taskId=$runId",
                PipelineRunTasks.tableDisplayFields,
                buttons = listOf("btnRun")
            )
        }
        setScript {
            postValue()
        }
    }
}