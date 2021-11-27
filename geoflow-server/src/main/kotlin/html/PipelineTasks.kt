package html

import database.enums.FileCollectType
import database.tables.PipelineRunTasks
import io.ktor.html.Template
import kotlinx.html.script
import kotlinx.html.HTML
import kotlinx.html.unsafe

/** Page for pipeline task operations */
object PipelineTasks {

    private const val taskDataModalId = "taskData"
    private const val taskTableId = "tasksTable"
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

    /**
     * Returns a [BasePage] with:
     * - 2 CSS classes
     * - tab layout with a basic table for pipeline task records with [tableButtons] (subscribed to given api endpoint
     * for table updates) and a table for the runs [SourceTables][database.tables.SourceTables]
     * - display modal for task details
     * - form modal for editing/creating source table records
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
            tabLayout(
                tabNav("Tasks") {
                    basicTable(
                        tableId = taskTableId,
                        dataUrl = "",
                        fields = PipelineRunTasks.tableDisplayFields,
                        tableButtons = tableButtons,
                        clickableRows = false,
                        subscriber = "ws://localhost:8080/api/pipeline-run-tasks/$runId",
                    )
                },
                tabNav("Source Tables") {
                    sourceTables(runId)
                },
            )
            dataDisplayModal(
                modalId = taskDataModalId,
                headerText = "Task Details",
            )
            sourceTableEditModal()
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
