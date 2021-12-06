package html

import database.tables.SourceTables
import kotlinx.html.FlowContent
import kotlinx.html.THEAD
import kotlinx.html.UL
import kotlinx.html.id
import kotlinx.html.script
import kotlinx.html.table
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import kotlinx.html.ul
import kotlinx.html.unsafe

/** Utility function to convert a JSON key to a display field name */
fun getFieldTable(field: String): String = field
    .replace("_", " ")
    .fold(StringBuilder()) { acc, c ->
        acc.append(if (acc.ifEmpty { " " }.last().isWhitespace()) c.titlecase() else c)
    }
    .toString()

/**
 * Template for buttons found in a table. Uses parameters to construct a JSON object entry.
 *
 * See [buttons](https://bootstrap-table.com/docs/api/table-options/#buttons) option for more details.
 */
fun tableButton(
    name: String,
    text: String,
    icon: String,
    event: String,
    title: String,
): String {
    /** json key value pair as text */
    return """
        $name: {
            text: '$text',
            icon: 'fa-${icon.replace("^fa-".toRegex(), "")}',
            event: () => { $event; },
            attributes: {
                title: '$title',
            },
        }
    """.trimIndent()
}

/**
 * Template for buttons found in a table. Uses the [name] as a key and the [html] code as the only entry in the object.
 * The html can be multi-line and should be directly what html you want to implement for the button.
 *
 * See [buttons](https://bootstrap-table.com/docs/api/table-options/#buttons) option for more details.
 */
fun tableButton(name: String, html: String): String {
    return """
        $name: {
            html: `$html`
        }
    """.trimIndent()
}

/** Container for table header buttons. Provides a [name] and [html] code for the html list entry */
data class HeaderButton(
    /** name of button */
    val name: String,
    /** code used to add the button to the ul tag */
    val html: UL.() -> Unit,
)

/** Adds a row to the `thead` tag and populates that row with the fields provided */
private fun THEAD.addFields(fields: Map<String, Map<String, String>>, clickableRows: Boolean) {
    tr {
        for ((field, options) in fields) {
            th {
                attributes["data-field"] = field
                if (clickableRows) {
                    attributes["data-cell-style"] = "clickableTd"
                }
                for ((key, value) in options) {
                    if (key != "name") {
                        attributes["data-$key"] = value
                    }
                }
                text(options["name"] ?: getFieldTable(field))
            }
        }
    }
}

/**
 * Constructs a basic [BootstrapTable](https://bootstrap-table.com) that fetches data from the desired [url][dataUrl],
 * displays the desired [fields] using the keys as reference to JSON data's keys. The [fields] parameter also allows the
 * user to add [column options](https://bootstrap-table.com/docs/api/column-options/) as a map of option to value.
 * Optional parameters include [table buttons](https://bootstrap-table.com/docs/api/table-options/#buttons),
 * [header buttons](https://bootstrap-table.com/docs/api/table-options/#toolbar), sort function (name of javascript
 * function to call), a flag denoting is the rows are [clickable][clickableRows], and a [subscriber] url.
 */
@Suppress("LongParameterList")
fun FlowContent.basicTable(
    tableId: String,
    fields: Map<String, Map<String, String>>,
    dataUrl: String = "",
    dataField: String = "",
    tableButtons: List<String> = emptyList(),
    headerButtons: List<HeaderButton> = emptyList(),
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
        if (dataUrl.isNotBlank()) {
            attributes["data-url"] = "http://localhost:8080/data/$dataUrl"
        }
        if (dataField.isNotBlank()) {
            attributes["data-data-field"] = dataField
        }
        if (subscriber.isBlank()) {
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
            addFields(fields, clickableRows)
        }
        if (tableButtons.isNotEmpty()) {
            script {
                unsafe {
                    raw("""
                        function ${tableId}buttons() {
                            return {
                                ${tableButtons.joinToString()}
                            }
                        }
                    """.trimIndent())
                }
            }
        }
    }
}

private const val SOURCE_TABLES_TABLE_ID = "sourceTables"

/** Constructs a basic table for source table data */
fun FlowContent.sourceTables(runId: Long) {
    basicTable(
        tableId = SOURCE_TABLES_TABLE_ID,
        dataUrl = "source-tables/$runId",
        dataField = "payload",
        fields = SourceTables.tableDisplayFields,
        tableButtons = listOf(
            tableButton(
                name = "btnAddTable",
                text = "Add Source Table",
                icon = "fa-plus",
                event = "newSourceTableRow()",
                title = "Add new source table to the current run",
            ),
        ),
        customSortFunction = "sourceTableRecordSorting",
        clickableRows = false,
    )
}
