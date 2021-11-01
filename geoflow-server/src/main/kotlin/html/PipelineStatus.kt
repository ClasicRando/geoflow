package html

import io.ktor.html.*
import kotlinx.html.*
import orm.tables.PipelineRuns

/** Page for pipeline status operations */
object PipelineStatus {

    private const val modalId = "selectReadyRun"
    private const val tableId = "runs"

    /**
     * Returns a [BasePage] with:
     * - a basic table for pipeline status records
     * - a basic modal for when the user can pick up a run not previously assigned to another user
     * - class level constants assigned to global javascript variables
     * - a specific script for this page loaded from assets
     */
    fun withWorkflowCode(workflowCode: String): Template<HTML> {
        return BasePage.withContent {
            basicTable(
                tableId,
                dataUrl = "/api/pipeline-runs/${workflowCode}",
                PipelineRuns.tableDisplayFields
            )
            basicModal(
                modalId,
                headerText = "Select Run",
                bodyMessage = "Pick up this run?",
                okClickFunction = "pickup"
            )
        }.withScript {
            script {
                addParamsAsJsGlobalVariables(
                    mapOf(
                        "modalId" to modalId,
                        "tableId" to tableId,
                    )
                )
            }
            script {
                src = "/assets/pipeline-status.js"
            }
        }
    }
}