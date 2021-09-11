package html

import orm.tables.PipelineRuns
import orm.tables.WorkflowOperations

class PipelineStatus(workflowCode: String): BasePage() {
    init {
        setContent {
            basicTable(
                WorkflowOperations.workflowName(workflowCode),
                "runs",
                "/api/pipeline-runs?code=$workflowCode",
                PipelineRuns.tableDisplayFields
            )
        }
    }
}