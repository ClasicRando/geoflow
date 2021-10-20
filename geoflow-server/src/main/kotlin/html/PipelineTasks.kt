package html

import kotlinx.html.*
import orm.enums.FileCollectType
import orm.tables.PipelineRunTasks

class PipelineTasks(runId: Long): BasePage() {
    private val taskDataModalId = "taskData"
    private val taskTableId = "tasks"
    private val webSocketPath = "ws://localhost:8080/sockets/pipeline-run-tasks/$runId"
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
    init {
        setStyles {
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
        setContent {
            basicTable(
                taskTableId,
                "/api/pipeline-run-tasks/$runId",
                PipelineRunTasks.tableDisplayFields,
                tableButtons = tableButtons,
                headerButtons = headerButtons,
                clickableRows = false,
                subscriber = webSocketPath,
            )
            dataDisplayModal(
                taskDataModalId,
                "Task Details",
            )
            sourceTablesModal(runId)
            messageBoxModal()
        }
        setScript {
            script {
                addParamsAsJsGlobalVariables(
                    mapOf(
                        "taskTableId" to taskTableId,
                        "taskDataModalId" to taskDataModalId,
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