package me.geoflow.web.pages

import me.geoflow.core.database.enums.FileCollectType
import me.geoflow.core.database.tables.PipelineRunTasks
import me.geoflow.web.html.addParamsAsJsGlobalVariables
import me.geoflow.web.html.basicTable
import me.geoflow.web.html.sourceTableEditModal
import me.geoflow.web.html.sourceTables
import me.geoflow.web.html.tabLayout
import me.geoflow.web.html.tabNav
import me.geoflow.web.html.tableButton
import kotlinx.html.FlowContent
import kotlinx.html.STYLE
import kotlinx.html.script
import kotlinx.html.unsafe
import me.geoflow.web.html.SubTableDetails

/**
 * Page for pipeline task operations
 *
 * Contains:
 * - 2 CSS classes
 * - tab layout with a basic table for pipeline task records with [tableButtons] (subscribed to given api endpoint
 * for table updates) and a table for the runs [SourceTables][me.geoflow.core.database.tables.SourceTables]
 * - display modal for task details
 * - form modal for editing/creating source table records
 * - class level constants assigned to global javascript variables named after the constant's names
 * - a specific script for this page loaded from assets
 */
class PipelineTasks(
    /** ID for the pipeline run to be displayed in this page */
    private val runId: Long,
) : BasePage() {

    override val styles: STYLE.() -> Unit = {
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
    }

    override val content: FlowContent.() -> Unit = {
        tabLayout(
            tabNav(label = "Tasks") {
                basicTable(
                    tableId = TASKS_TABLE_ID,
                    dataUrl = "",
                    fields = PipelineRunTasks.tableDisplayFields,
                    tableButtons = tableButtons,
                    clickableRows = false,
                    subscriber = "ws://localhost:8080/data/pipeline-run-tasks/$runId",
                    subTableDetails = SubTableDetails(
                        fields = PipelineRunTasks.subTableDisplayFields,
                    ),
                )
            },
            tabNav(label = "Source Tables") {
                sourceTables(runId)
            },
        )
        sourceTableEditModal()
    }

    override val script: FlowContent.() -> Unit = {
        script {
            addParamsAsJsGlobalVariables(
                "taskTableId" to TASKS_TABLE_ID,
                "taskDataModalId" to TASK_DATA_MODAL_ID,
                "types" to FileCollectType.values(),
            )
        }
        script {
            src = "/assets/pipeline-tasks.js"
        }
    }

    companion object {

        private const val TASK_DATA_MODAL_ID = "taskData"
        private const val TASKS_TABLE_ID = "tasksTable"
        private val tableButtons = listOf(
            tableButton(
                name = "btnTimeUnit",
                text = "Switch Time Unit",
                icon = "clock",
                event = "changeTimeUnit()",
                title = "Switch between minutes and seconds",
            ),
            tableButton(
                name = "btnRun",
                text = "Run Next Task",
                icon = "play",
                event = "clickRunTask()",
                title = "Run the next available task if there are no other tasks running",
            ),
            tableButton(
                name = "btnRunAll",
                text = "Run All Tasks",
                icon = "fast-forward",
                event = "clickRunAllTasks()",
                title = """
                    Run the next available tasks if there are no other tasks running. Stops upon failure or User Task
                """.trimIndent(),
            ),
            tableButton(
                name = "btnConnected",
                html = """
                    <button class="btn btn-secondary" name="btnConnected"
                        title="Shows if the subscriber is active. Click to attempt restart if inactive">
                        <span class="fa-layers fa-fw">
                            <i class="fas fa-plug"></i>
                            <i class="fas fa-slash"></i>
                        </span>
                    </button>
                """.trimIndent(),
            ),
        )

    }

}
