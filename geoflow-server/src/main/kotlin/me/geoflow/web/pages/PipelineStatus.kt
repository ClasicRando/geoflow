package me.geoflow.web.pages

import io.ktor.application.ApplicationCall
import me.geoflow.core.database.tables.PipelineRuns
import me.geoflow.core.web.html.addParamsAsJsGlobalVariables
import kotlinx.html.FlowContent
import kotlinx.html.STYLE
import kotlinx.html.id
import kotlinx.html.li
import kotlinx.html.script
import kotlinx.html.select
import me.geoflow.core.web.html.JSElement
import me.geoflow.web.api.NoBody
import me.geoflow.web.api.makeApiCall
import me.geoflow.core.web.html.basicTable
import me.geoflow.core.web.html.confirmModal
import me.geoflow.web.session

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
    /** Call context used to extract the session */
    private val call: ApplicationCall,
) : BasePage() {

    override val styles: STYLE.() -> Unit = {}

    override val content: FlowContent.() -> Unit = {
        basicTable<PipelineRuns>(
            tableId = TABLE_ID,
            dataUrl = "pipeline-runs/$workflowCode",
            dataField = "payload",
            clickableRows = false,
        ) {
            li {
                select(classes = "custom-select") {
                    id = STATUS_SELECT
                }
            }
        }
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

    override val script: suspend FlowContent.() -> Unit = {
        val session = call.session
        requireNotNull(session)
        val operationsJson = makeApiCall<NoBody, String>(
            endPoint = "/api/operations/data",
            apiToken = session.apiToken,
        )
        script {
            addParamsAsJsGlobalVariables(
                "confirmPickup" to JSElement(query = "#$CONFIRM_PICKUP", makeJQuery = false),
                "table" to JSElement(query = "#$TABLE_ID"),
                "confirmForward" to JSElement(query = "#$CONFIRM_FORWARD"),
                "confirmBack" to JSElement(query = "#$CONFIRM_BACK"),
                "operationsJson" to operationsJson,
                "statusSelect" to JSElement(query = "#$STATUS_SELECT", makeJQuery = false),
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
        private const val STATUS_SELECT = "status"
    }

}
