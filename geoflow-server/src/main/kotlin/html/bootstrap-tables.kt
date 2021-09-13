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
                alert('btnRun pressed');
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