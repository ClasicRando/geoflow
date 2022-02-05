package me.geoflow.web.pages

import kotlinx.html.FlowContent
import kotlinx.html.STYLE
import kotlinx.html.script
import kotlinx.html.unsafe
import me.geoflow.core.database.enums.FileCollectType
import me.geoflow.core.database.tables.PipelineRunTasks
import me.geoflow.core.web.html.JSElement
import me.geoflow.core.web.html.SubTableDetails
import me.geoflow.core.web.html.addParamsAsJsGlobalVariables
import me.geoflow.core.web.html.basicTable
import me.geoflow.core.web.html.emptyModal
import me.geoflow.core.web.html.tabLayout
import me.geoflow.core.web.html.tabNav
import me.geoflow.core.web.html.tableButton
import me.geoflow.web.html.plottingFields
import me.geoflow.web.html.plottingMethods
import me.geoflow.web.html.sourceTables
import me.geoflow.web.pages.PipelineTasks.Companion.tableButtons

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
                basicTable<PipelineRunTasks>(
                    tableId = TASKS_TABLE_ID,
                    dataUrl = "",
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
            tabNav(label = "Plotting Fields") {
                plottingFields(runId)
            },
            tabNav(label = "Plotting Methods") {
                plottingMethods(runId)
            }
        )
        emptyModal(
            modalId = TASK_OUTPUT_MODAL,
            headerText = "Task Output",
            size = "modal-xl",
        )
    }

    override val script: suspend FlowContent.() -> Unit = {
        script {
            addParamsAsJsGlobalVariables(
                "taskTableId" to TASKS_TABLE_ID,
                "types" to FileCollectType.values(),
                "taskOutput" to JSElement(id = TASK_OUTPUT_MODAL, makeElement = false),
            )
        }
        script {
            src = "/assets/pipeline-tasks.js"
        }
    }

    companion object {

        private const val TASKS_TABLE_ID = "tasksTable"
        private const val TASK_OUTPUT_MODAL = "taskOutput"
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
                event = "clickRunTask(false)",
                title = "Run the next available task if there are no other tasks running",
            ),
            tableButton(
                name = "btnRunAll",
                text = "Run All Tasks",
                icon = "fast-forward",
                event = "clickRunTask(true)",
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
