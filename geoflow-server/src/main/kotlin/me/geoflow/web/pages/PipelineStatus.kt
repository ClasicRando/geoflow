package me.geoflow.web.pages

import me.geoflow.core.database.tables.PipelineRuns
import me.geoflow.web.html.addParamsAsJsGlobalVariables
import me.geoflow.web.html.basicTable
import kotlinx.html.FlowContent
import kotlinx.html.STYLE
import kotlinx.html.script
import me.geoflow.web.html.confirmModal

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
        confirmModal(
            confirmModalId = CONFIRM_PICKUP,
            confirmMessage = "Pick up this run?",
            resultFunction = "pickup()"
        )
        confirmModal(
            confirmModalId = CONFIRM_FORWARD,
            confirmMessage = "Move this run to the next state?",
            resultFunction = "forward()"
        )
        confirmModal(
            confirmModalId = CONFIRM_BACK,
            confirmMessage = "Move this run to the previous state?",
            resultFunction = "back()"
        )
    }

    override val script: FlowContent.() -> Unit = {
        script {
            addParamsAsJsGlobalVariables(
                "confirmPickupId" to CONFIRM_PICKUP,
                "tableId" to TABLE_ID,
                "confirmForwardId" to CONFIRM_FORWARD,
                "confirmBackId" to CONFIRM_BACK
            )
        }
        script {
            src = "/assets/pipeline-status.js"
        }
    }

    companion object {

        private const val CONFIRM_PICKUP = "confirmPickup"
        private const val CONFIRM_FORWARD = "confirmForward"
        private const val CONFIRM_BACK = "confirmBack"
        private const val TABLE_ID = "runs"

    }

}
