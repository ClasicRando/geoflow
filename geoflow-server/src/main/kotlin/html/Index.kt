package html

import kotlinx.html.div
import kotlinx.html.script
import kotlinx.html.unsafe
import orm.tables.Actions
import orm.tables.WorkflowOperations

class Index: BasePage() {
    private val operationsTableId = "operations"
    private val actionsTableId = "actions"
    init {
        setContent {
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
        }
        setScript {
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
}