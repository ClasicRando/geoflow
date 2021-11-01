package html

import kotlinx.html.*
import orm.tables.Actions
import orm.tables.WorkflowOperations

/** Page for initial page upon login */
object Index {
    private const val operationsTableId = "operations"
    private const val actionsTableId = "actions"

    /**
     * A [BasePage] instance with:
     * - a basic table for workflow operations
     * - a basic table for user actions
     * - javascript raw code for registering table row click events to a post function found in 'utils.js'
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
            unsafe {
                raw("""
                    $('#$operationsTableId').on('click-row.bs.table', (e, row, element, field) => { post(row) });
                    $('#$actionsTableId').on('click-row.bs.table', (e, row, element, field) => { post(row) });
                """.trimIndent())
            }
        }
    }
}