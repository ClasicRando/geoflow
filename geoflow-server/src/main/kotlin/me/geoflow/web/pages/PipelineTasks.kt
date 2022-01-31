package me.geoflow.web.pages

import io.ktor.application.ApplicationCall
import kotlinx.html.ButtonType
import me.geoflow.core.database.enums.FileCollectType
import me.geoflow.core.database.tables.PipelineRunTasks
import me.geoflow.core.web.html.addParamsAsJsGlobalVariables
import me.geoflow.web.html.sourceTables
import me.geoflow.core.web.html.tabLayout
import me.geoflow.core.web.html.tabNav
import me.geoflow.core.web.html.tableButton
import kotlinx.html.FlowContent
import kotlinx.html.STYLE
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.i
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.ol
import kotlinx.html.onClick
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.unsafe
import me.geoflow.core.database.tables.PlottingFields
import me.geoflow.core.database.tables.PlottingMethods
import me.geoflow.core.web.html.SubTableDetails
import me.geoflow.core.web.html.basicModal
import me.geoflow.core.web.html.basicTable
import me.geoflow.core.web.html.confirmModal
import me.geoflow.core.web.html.emptyModal
import me.geoflow.core.web.html.formModal
import me.geoflow.core.web.html.tabLayout
import me.geoflow.core.web.html.tabNav
import me.geoflow.core.web.html.tableButton
import me.geoflow.web.api.NoBody
import me.geoflow.web.api.makeApiCall
import me.geoflow.web.html.sourceTables
import me.geoflow.web.session

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
    /** Call context used to extract the session */
    private val call: ApplicationCall,
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
                basicTable<PlottingFields>(
                    tableId = PLOTTING_FIELDS_TABLE,
                    dataUrl = "plotting-fields/${runId}",
                    dataField = "payload",
                    clickableRows = false,
                )
                confirmModal(
                    confirmModalId = CONFIRM_DELETE_PLOTTING_FIELDS,
                    confirmMessage = "Are you sure you want to delete a plotting fields record?",
                    resultFunction = "deletePlottingFields()",
                )
            },
            tabNav(label = "Plotting Methods") {
                basicTable<PlottingMethods>(
                    tableId =  PLOTTING_METHODS_TABLE,
                    dataUrl = "plotting-methods/${runId}",
                    dataField = "payload",
                    tableButtons = plottingMethodButtons,
                    clickableRows = false,
                )
                basicModal(
                    modalId = PLOTTING_METHODS_MODAL,
                    headerText = "Plotting Methods",
                    okClickFunction = "setPlottingFields($('#${PLOTTING_METHODS_MODAL}'))",
                    size = "modal-xl",
                ) {
                    button(classes = "btn btn-secondary") {
                        type = ButtonType.button
                        onClick = "addPlottingMethod()"
                        +"Add Method"
                        i(classes = "fas fa-plus p-1")
                    }
                    ol(classes = "list-group") {

                    }
                }
            }
        )
        formModal(
            modalId = PLOTTING_FIELDS_MODAL,
            headerText = "Plotting Fields",
            okClickFunction = "submitPlottingFields($('#${PLOTTING_FIELDS_MODAL}'))",
            resetFormButton = true,
        ) {
            div(classes = "form-group") {
                label {
                    htmlFor = "mergeKey"
                    +"Merge Key"
                }
                select(classes = "custom-select") {
                    id = "mergeKey"
                    name = "mergeKey"
                }
            }
            div(classes = "form-group") {
                label {
                    htmlFor = "companyName"
                    +"Company Name"
                }
                select(classes = "custom-select") {
                    id = "companyName"
                    name = "companyName"
                }
            }
            div(classes = "form-group") {
                label {
                    htmlFor = "addressLine1"
                    +"Address Line 1"
                }
                select(classes = "custom-select") {
                    id = "addressLine1"
                    name = "addressLine1"
                }
            }
            div(classes = "form-group") {
                label {
                    htmlFor = "addressLine2"
                    +"Address Line 2"
                }
                select(classes = "custom-select") {
                    id = "addressLine2"
                    name = "addressLine2"
                }
            }
            div(classes = "form-group") {
                label {
                    htmlFor = "city"
                    +"City"
                }
                select(classes = "custom-select") {
                    id = "city"
                    name = "city"
                }
            }
            div(classes = "form-group") {
                label {
                    htmlFor = "alternateCities"
                    +"Alternate Cities"
                }
                select(classes = "custom-select") {
                    id = "alternateCities"
                    name = "alternateCities"
                    multiple = true
                }
            }
            div(classes = "form-group") {
                label {
                    htmlFor = "mailCode"
                    +"Mail Code"
                }
                select(classes = "custom-select") {
                    id = "mailCode"
                    name = "mailCode"
                }
            }
            div(classes = "form-group") {
                label {
                    htmlFor = "prov"
                    +"Prov/State"
                }
                select(classes = "custom-select") {
                    id = "prov"
                    name = "prov"
                }
            }
            div(classes = "form-group") {
                label {
                    htmlFor = "latitude"
                    +"Latitude"
                }
                select(classes = "custom-select") {
                    id = "latitude"
                    name = "latitude"
                }
            }
            div(classes = "form-group") {
                label {
                    htmlFor = "longitude"
                    +"Longitude"
                }
                select(classes = "custom-select") {
                    id = "longitude"
                    name = "longitude"
                }
            }
        }
        emptyModal(
            modalId = TASK_OUTPUT_MODAL,
            headerText = "Task Output",
            size = "modal-xl",
        )
    }

    override val script: suspend FlowContent.() -> Unit = {
        val session = call.session
        requireNotNull(session)
        val operationsJson = makeApiCall<NoBody, String>(
            endPoint = "/api/plotting-method-types",
            apiToken = session.apiToken,
        )
        script {
            addParamsAsJsGlobalVariables(
                "taskTableId" to TASKS_TABLE_ID,
                "taskDataModalId" to TASK_DATA_MODAL_ID,
                "types" to FileCollectType.values(),
                "taskOutputId" to TASK_OUTPUT_MODAL,
                "plottingFieldsModalId" to PLOTTING_FIELDS_MODAL,
                "plottingFieldsTableId" to PLOTTING_FIELDS_TABLE,
                "confirmDeletePlottingFieldsId" to CONFIRM_DELETE_PLOTTING_FIELDS,
                "plottingMethodsModalId" to PLOTTING_METHODS_MODAL,
            )
        }
        script {
            src = "/assets/pipeline-tasks.js"
        }
    }

    companion object {

        private const val TASK_DATA_MODAL_ID = "taskData"
        private const val TASKS_TABLE_ID = "tasksTable"
        private const val TASK_OUTPUT_MODAL = "taskOutput"
        private const val PLOTTING_METHODS_TABLE = "plottingMethodsTable"
        private const val PLOTTING_METHODS_MODAL = "plottingMethodsModal"
        private const val PLOTTING_FIELDS_MODAL = "plottingFieldsModal"
        private const val PLOTTING_FIELDS_TABLE = "plottingFieldsTable"
        private const val CONFIRM_DELETE_PLOTTING_FIELDS = "confirmDeletePlottingFields"
        private val plottingMethodButtons = listOf(
            tableButton(
                name = "btnEditPlottingMethods",
                text = "Edit Plotting Methods",
                icon = "edit",
                event = "editPlottingMethods()",
                title = "Edit the current plotting methods for the run",
            )
        )
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
