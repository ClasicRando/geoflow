package html

import database.tables.Actions
import database.tables.WorkflowOperations
import kotlinx.html.div
import kotlinx.html.script

/** Page for initial page upon login */
object Index {
    private const val operationsTableId = "operations"
    private const val actionsTableId = "actions"

    /**
     * A [BasePage] instance with:
     * - a basic table for workflow operations
     * - a basic table for user actions
     * - mapping object properties to javascript global variables named after the property name
     * - link to static asset for registering table row click events to navigate to the desired href
     */
    val page = BasePage.withContent {
        div(classes = "row") {
            div(classes = "col") {
                basicTable(
                    operationsTableId,
                    "/api/operations",
                    WorkflowOperations.tableDisplayFields
                )
            }
            div(classes = "col") {
                basicTable(
                    actionsTableId,
                    "/api/actions",
                    Actions.tableDisplayFields
                )
            }
        }
    }.withScript {
        script {
            addParamsAsJsGlobalVariables(
                mapOf(
                    ::operationsTableId.name to operationsTableId,
                    ::actionsTableId.name to actionsTableId,
                )
            )
        }
        script {
            src = "/assets/index.js"
        }
    }
}
