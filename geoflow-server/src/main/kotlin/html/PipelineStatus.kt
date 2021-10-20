package html

import kotlinx.html.*
import orm.tables.PipelineRuns

class PipelineStatus(workflowCode: String): BasePage() {
    private val modalId = "selectReadyRun"
    private val tableId = "runs"
    init {
        setContent {
            basicTable(
                tableId,
                "/api/pipeline-runs/$workflowCode",
                PipelineRuns.tableDisplayFields
            )
            basicModal(
                modalId,
                "Select Run",
                "Pickup this run to collect?",
                "pickup"
            )
        }
        setScript {
            script {
                unsafe {
                    raw("""
                        var modalId = '$modalId';
                        var tableId = '$tableId';
                    """.trimIndent())
                }
            }
            script {
                src = "/assets/pipeline-status.js"
            }
        }
    }
}