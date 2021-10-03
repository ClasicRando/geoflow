package html

import kotlinx.html.*
import orm.enums.FileCollectType
import orm.tables.PipelineRunTasks

class PipelineTasks(runId: Long): BasePage() {
    private val taskDataModalId = "taskData"
    private val sourceTableModalId = "sourceTableData"
    private val taskTableId = "tasks"
    private val sourceTablesTableId = "source-tables"
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
            autoRefreshTable(
                taskTableId,
                "/api/pipeline-run-tasks?runId=$runId",
                PipelineRunTasks.tableDisplayFields,
                tableButtons = tableButtons,
                headerButtons = headerButtons,
            )
            dataDisplayModal(
                taskDataModalId,
                "Task Details",
            )
            sourceTablesModal(
                sourceTableModalId,
                sourceTablesTableId,
                "/api/source-tables?runId=$runId",
            )
            messageBoxModal()
        }
        setScript {
            script {
                src = "https://unpkg.com/bootstrap-table@1.18.3/dist/extensions/auto-refresh/bootstrap-table-auto-refresh.js"
            }
            script {
                unsafe {
                    raw("""
                        var taskTableId = '$taskTableId';
                        var taskDataModalId = '$taskDataModalId';
                        var sourceTablesTableId = '$sourceTablesTableId';
                        var sourceTableModalId = '$sourceTableModalId';
                        var types = [${FileCollectType.values().joinToString("','", "'", "'")}];
                    """.trimIndent())
                }
            }
            script {
                src = "/assets/pipeline-tasks.js"
            }
        }
    }
}