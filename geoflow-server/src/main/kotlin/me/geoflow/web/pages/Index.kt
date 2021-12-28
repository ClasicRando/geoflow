package me.geoflow.web.pages

import me.geoflow.core.database.tables.Actions
import me.geoflow.core.database.tables.WorkflowOperations
import me.geoflow.web.html.addParamsAsJsGlobalVariables
import kotlinx.html.FlowContent
import kotlinx.html.STYLE
import kotlinx.html.div
import kotlinx.html.script
import me.geoflow.web.html.basicTable

/** Page for initial page upon login */
object Index : BasePage() {

    private const val OPERATIONS_TABLE_ID = "operations"
    private const val ACTIONS_TABLE_ID = "actions"

    override val styles: STYLE.() -> Unit = {}

    override val content: FlowContent.() -> Unit = {
        div(classes = "row") {
            div(classes = "col") {
                basicTable<WorkflowOperations>(
                    tableId = OPERATIONS_TABLE_ID,
                    dataUrl = "operations",
                    dataField = "payload",
                )
            }
            div(classes = "col") {
                basicTable<Actions>(
                    tableId = ACTIONS_TABLE_ID,
                    dataUrl = "actions",
                    dataField = "payload",
                )
            }
        }
    }

    override val script: FlowContent.() -> Unit = {
        script {
            addParamsAsJsGlobalVariables(
                "operationsTableId" to OPERATIONS_TABLE_ID,
                "actionsTableId" to ACTIONS_TABLE_ID,
            )
        }
        script {
            src = "/assets/index.js"
        }
    }

}
