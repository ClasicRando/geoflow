package me.geoflow.web.pages

import me.geoflow.core.database.tables.PipelineRuns
import me.geoflow.web.html.addParamsAsJsGlobalVariables
import me.geoflow.web.html.basicModal
import me.geoflow.web.html.basicTable
import kotlinx.html.FlowContent
import kotlinx.html.STYLE
import kotlinx.html.script

/**
 * Page for pipeline status operations
 *
 * Contains:
 * - a basic table for pipeline status records
 * - a basic modal for when the user can pick up a run not previously assigned to another user
 * - class level constants assigned to global javascript variables named after the constant's names
 * - a specific script for this page loaded from assets
 */
class PipelineStatus(
    /** Code of the workflow operation to be displayed in this page. Only displays runs currently in this state */
    private val workflowCode: String,
) : BasePage() {

    override val styles: STYLE.() -> Unit = {}

    override val content: FlowContent.() -> Unit = {
        basicTable(
            tableId = TABLE_ID,
            dataUrl = "pipeline-runs/$workflowCode",
            dataField = "payload",
            fields = PipelineRuns.tableDisplayFields,
            clickableRows = false,
        )
        basicModal(
            modalId = MODAL_ID,
            headerText = "Select Run",
            bodyMessage = "Pick up this run?",
            okClickFunction = "pickup"
        )
    }

    override val script: FlowContent.() -> Unit = {
        script {
            addParamsAsJsGlobalVariables(
                "modalId" to MODAL_ID,
                "tableId" to TABLE_ID,
            )
        }
        script {
            src = "/assets/pipeline-status.js"
        }
    }

    companion object {

        private const val MODAL_ID = "selectReadyRun"
        private const val TABLE_ID = "runs"

    }

}
