package html

import kotlinx.html.*

fun getFieldTable(field: String): String = field
    .replace("_", " ")
    .fold(StringBuilder()) { acc, c ->
        acc.append(if (acc.ifEmpty { " " }.last().isWhitespace()) c.titlecase() else c)
    }
    .toString()

val availableButtons = mapOf(
    "btnRun" to """
        btnRun: {
            text: 'Run Next Task',
            icon: 'fa-play',
            event: () => {
                let ${'$'}table = $('#tasks');
                const params = new URLSearchParams(window.location.href.replace(/^[^?]+/g, ''));
                let data = ${'$'}table.bootstrapTable('getData');
                if (data.find(row => row.task_status === 'Running' || row.task_status === 'Scheduled') != undefined) {
                    alert('Task already running');
                    return;
                }
                let row = data.find(row => row.task_status === 'Waiting');
                if (row != undefined) {
                   postValue(`/api/run-task?runId=${'$'}{params.get('runId')}&prTaskId=${'$'}{row.pipeline_run_task_id}`);
                   ${'$'}table.bootstrapTable('refresh');
                } else {
                    alert('No Task to run');
                }
            },
            attributes: {
                title: 'Run the next available task if there is no other tasks running'
            }
        }
    """.trimIndent()
)

fun FlowContent.basicTable(
    tableName: String,
    tableId: String,
    dataUrl: String,
    fields: Map<String, Map<String, String>>,
    buttons: List<String> = listOf(),
) {
    h3 {
        +tableName
    }
    table {
        id = tableId
        attributes["data-toggle"] = "table"
        attributes["data-url"] = dataUrl
        attributes["data-show-refresh"] = "true"
        attributes["data-classes"] = "table table-bordered table-hover"
        attributes["data-thead-classes"] = "thead-dark"
        attributes["data-search"] = "true"
        if (buttons.isNotEmpty()) {
            attributes["data-buttons"] = "buttons"
        }
        thead {
            tr {
                fields.forEach { (field, options) ->
                    th {
                        attributes["data-field"] = field
                        options.filter { it.key != "name" }.forEach { (key, value) ->
                            attributes["data-$key"] = value
                        }
                        text(options["name"] ?: getFieldTable(field))
                    }
                }
            }
        }
        if (buttons.isNotEmpty()) {
            script {
                unsafe {
                    raw("""
                        function buttons() {
                            return {
                                ${
                                    buttons
                                        .filter { name -> name in availableButtons }
                                        .joinToString { name -> availableButtons[name]!! }
                                }
                            }
                        }
                    """.trimIndent())
                }
            }
        }
    }
}