package html

import kotlinx.html.script
import kotlinx.html.unsafe
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
        setScript {
            postObject
            script {
                unsafe {
                    raw("$('#runs').on('click-row.bs.table', (e, row, element, field) => { post(row) });")
                }
            }
        }
    }
}