package html

import kotlinx.html.*

/** Utility function to convert a JSON key to a display field name */
fun getFieldTable(field: String): String = field
    .replace("_", " ")
    .fold(StringBuilder()) { acc, c ->
        acc.append(if (acc.ifEmpty { " " }.last().isWhitespace()) c.titlecase() else c)
    }
    .toString()

/**
 * Container template for buttons found in a table. Uses properties to construct JSON entry.
 *
 * See [buttons](https://bootstrap-table.com/docs/api/table-options/#buttons) option for more details.
 */
data class TableButton(val name: String, val text: String, val icon: String, val event: String, val title: String) {
    val button = """
        $name: {
            text: '$text',
            icon: '$icon',
            event: () => { $event; },
            attributes: {
                title: '$title',
            },
        }
    """.trimIndent()
}

/** Container for table header buttons. Provides a [name] and [html code][html] for the html list entry */
data class HeaderButton(val name: String, val html: UL.() -> Unit)

/**
 * Constructs a basic [BootstrapTable](https://bootstrap-table.com) that fetches data from the desired [url][dataUrl],
 * displays the desired [fields] using the keys as reference to JSON data's keys. The [fields] parameter also allows the
 * user to add [column options](https://bootstrap-table.com/docs/api/column-options/) as a map of option to value.
 * Optional parameters include [table buttons](https://bootstrap-table.com/docs/api/table-options/#buttons),
 * [header buttons](https://bootstrap-table.com/docs/api/table-options/#toolbar), sort function (name of javascript
 * function to call), a flag denoting is the rows are [clickable][clickableRows], and a [subscriber] url.
 */
fun FlowContent.basicTable(
    tableId: String,
    dataUrl: String,
    fields: Map<String, Map<String, String>>,
    tableButtons: List<TableButton> = listOf(),
    headerButtons: List<HeaderButton> = listOf(),
    customSortFunction: String = "",
    clickableRows: Boolean = true,
    subscriber: String = "",
) {
    if (headerButtons.isNotEmpty()) {
        ul(classes = "header-button-list") {
            id = "toolbar"
            for (headerButton in headerButtons) {
                headerButton.html(this)
            }
        }
    }
    table {
        id = tableId
        attributes["data-toggle"] = "table"
        if (headerButtons.isNotEmpty()) {
            attributes["data-toolbar"] = "#toolbar"
        }
        attributes["data-url"] = dataUrl
        if (subscriber.isEmpty()) {
            attributes["data-show-refresh"] = "true"
        } else {
            attributes["data-sub"] = "true"
            attributes["data-sub-url"] = subscriber
        }
        attributes["data-classes"] = "table table-bordered${if (clickableRows) " table-hover" else ""}"
        attributes["data-thead-classes"] = "thead-dark"
        attributes["data-search"] = "true"
        if (customSortFunction.isNotBlank()) {
            attributes["data-custom-sort"] = customSortFunction.trim()
        }
        if (tableButtons.isNotEmpty()) {
            attributes["data-buttons"] = "${tableId}buttons"
        }
        thead {
            tr {
                fields.forEach { (field, options) ->
                    th {
                        attributes["data-field"] = field
                        if (clickableRows) {
                            attributes["data-cell-style"] = "clickableTd"
                        }
                        options.filter { it.key != "name" }.forEach { (key, value) ->
                            attributes["data-$key"] = value
                        }
                        text(options["name"] ?: getFieldTable(field))
                    }
                }
            }
        }
        if (tableButtons.isNotEmpty()) {
            script {
                unsafe {
                    raw("""
                        function ${tableId}buttons() {
                            return {
                                ${tableButtons.joinToString { it.button }}
                            }
                        }
                    """.trimIndent())
                }
            }
        }
    }
}
