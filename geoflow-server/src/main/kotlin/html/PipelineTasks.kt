package html

import database.enums.FileCollectType
import database.tables.PipelineRunTasks
import io.ktor.html.Template
import kotlinx.html.script
import kotlinx.html.li
import kotlinx.html.button
import kotlinx.html.onClick
import kotlinx.html.HTML
import kotlinx.html.unsafe

/** Page for pipeline task operations */
object PipelineTasks {

    private const val taskDataModalId = "taskData"
    private const val taskTableId = "tasks"
    private val tableButtons = listOf(
        TableButton(
            "btnRun",
            "Run Next Task",
            "fa-play",
            "clickRunTask()",
            "Run the next available task if there is no other tasks running",
        ),
        TableButton(
            "btnRunAll",
            "Run All Tasks",
            "fa-fast-forward",
            "clickRunAllTasks()",
            "Run the next available tasks if there is no other tasks running. Stops upon task failure or User Task",
        ),
    )
    private val headerButtons = listOf(
        HeaderButton("btnSourceTables") {
            li(classes = "header-button") {
                button(classes = "btn btn-secondary") {
                    onClick = "showSourceTables()"
                    +"Source Tables"
                }
            }
        }
    )

    /**
     * Returns a [BasePage] with:
     * - 2 CSS classes
     * - basic table for pipeline task records with various [tableButtons] and [headerButtons]. Subscribed to given
     * WebSocket for table updates
     * - display modal for task details
     * - modal for source tables (see [sourceTablesModal] for details)
     * - messagebox modal
     * - class level constants assigned to global javascript variables named after the constant's names
     * - a specific script for this page loaded from assets
     */
    fun withRunId(runId: Long): Template<HTML> {
        return BasePage.withStyles {
            unsafe {
                raw("""
                .header-button-list {
                    margin 0;
                    padding 0;
                }
                .header-button {
                    margin-right 10px;
                    padding: 0 10px;
                    display: inline-block;
                }
            """.trimIndent())
            }
        }.withContent {
            basicTable(
                taskTableId,
                "/api/pipeline-run-tasks/$runId",
                PipelineRunTasks.tableDisplayFields,
                tableButtons = tableButtons,
                headerButtons = headerButtons,
                clickableRows = false,
                subscriber = "ws://localhost:8080/sockets/pipeline-run-tasks/$runId",
            )
            dataDisplayModal(
                taskDataModalId,
                "Task Details",
            )
            sourceTablesModal(runId)
            messageBoxModal()
        }.withScript {
            script {
                addParamsAsJsGlobalVariables(
                    mapOf(
                        ::taskTableId.name to taskTableId,
                        ::taskDataModalId.name to taskDataModalId,
                        "types" to FileCollectType.values(),
                    )
                )
            }
            script {
                src = "/assets/pipeline-tasks.js"
            }
        }
    }
}
