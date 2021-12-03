package html

import database.tables.PipelineRuns
import io.ktor.html.Template
import kotlinx.html.HTML
import kotlinx.html.script

/** Page for pipeline status operations */
object PipelineStatus {

    private const val modalId = "selectReadyRun"
    private const val tableId = "runs"

    /**
     * Returns a [BasePage] with:
     * - a basic table for pipeline status records
     * - a basic modal for when the user can pick up a run not previously assigned to another user
     * - class level constants assigned to global javascript variables named after the constant's names
     * - a specific script for this page loaded from assets
     */
    fun withWorkflowCode(workflowCode: String): Template<HTML> {
        return BasePage.withContent {
            basicTable(
                tableId = tableId,
                dataUrl = "pipeline-runs/$workflowCode",
                dataField = "payload",
                fields = PipelineRuns.tableDisplayFields,
                clickableRows = false,
            )
            basicModal(
                modalId = modalId,
                headerText = "Select Run",
                bodyMessage = "Pick up this run?",
                okClickFunction = "pickup"
            )
        }.withScript {
            script {
                addParamsAsJsGlobalVariables(
                    ::modalId.name to modalId,
                    ::tableId.name to tableId,
                )
            }
            script {
                src = "/assets/pipeline-status.js"
            }
        }
    }
}
