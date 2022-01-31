package me.geoflow.core.web.html

import kotlinx.html.FlowContent
import kotlinx.html.TABLE
import kotlinx.html.THEAD
import kotlinx.html.UL
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.table
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import kotlinx.html.ul
import kotlinx.html.unsafe
import me.geoflow.core.database.tables.ApiExposed
import me.geoflow.core.utils.getObjectInstance

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

/** Adds a row to the `thead` tag and populates that row with the fields provided */
fun THEAD.addFields(fields: Map<String, Map<String, String>>, clickableRows: Boolean) {
    tr {
        for ((field, options) in fields) {
            th {
                attributes["data-field"] = field
                if (clickableRows) {
                    attributes["data-cell-style"] = "clickableTd"
                }
                for ((key, value) in options) {
                    if (key != "title") {
                        attributes["data-$key"] = value
                    }
                }
                text(options["title"] ?: getFieldTable(field))
            }
        }
    }
}

/** Container for sub table details when allowing a bootstrap table record to show details */
data class SubTableDetails(
    /** Web service endpoint to collect the sub table data. Only include the portion after '/data/' */
    val url: String? = null,
    /** Name of id field within the row object. Used to complete the API query */
    val idField: String = "",
    /** Fields to show in the sub table. Same structure as normal main table fields */
    val fields: Map<String, Map<String, String>>,
)

/** Function to get a [SubTableDetails] instance using an [ApiExposed] table */
inline fun <reified T: ApiExposed> subTableDetails(url: String, idField: String): SubTableDetails {
    return SubTableDetails(
        url = url,
        idField = idField,
        fields = getObjectInstance<T>().tableDisplayFields,
    )
}

/** Used the provided [details] to add attributes to the table for setting up the sub table actions */
fun TABLE.applySubTabDetails(details: SubTableDetails) {
    attributes["data-detail-view"] = "true"
    if (details.url != null) {
        attributes["data-sub-table-url"] = "http://localhost:8080/data/${details.url}"
        attributes["data-sub-table-id"] = details.idField
    }
    for ((i, subField) in details.fields.entries.withIndex()) {
        val (field, options) = subField
        val serializedOptions = options.entries.joinToString(separator = "&") { "${it.key}=${it.value}" }
        attributes["data-sub-table-field$i"] = "field=${field}&$serializedOptions".trimEnd('&')
    }
}

/** Creates a javascript function that returns all the [tableButtons] provided. Function is named using the [tableId] */
fun TABLE.addTableButtons(tableId: String, tableButtons: List<String>) {
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

/** */
val emptyToolbar: UL.() -> Unit = {}

/**
 * Constructs a basic [BootstrapTable](https://bootstrap-table.com) that fetches data from the desired [url][dataUrl],
 * displays the field from [T] using the keys as reference to JSON data's keys. The fields obtained from [T] also allows
 * the user to add [column options](https://bootstrap-table.com/docs/api/column-options/) as a map of option to value.
 * Optional parameters include [table buttons](https://bootstrap-table.com/docs/api/table-options/#buttons),
 * [header buttons](https://bootstrap-table.com/docs/api/table-options/#toolbar), sort function (name of javascript
 * function to call), a flag denoting is the rows are [clickable][clickableRows], and a [subscriber] url.
 */
@Suppress("LongParameterList")
inline fun <reified T: ApiExposed> FlowContent.basicTable(
    tableId: String,
    dataUrl: String = "",
    dataField: String = "",
    tableButtons: List<String> = emptyList(),
    customSortFunction: String = "",
    clickableRows: Boolean = true,
    subscriber: String = "",
    subTableDetails: SubTableDetails? = null,
    freezeColumnsStart: Int? = null,
    freezeColumnsEnd: Int? = null,
    noinline toolbar: UL.() -> Unit = emptyToolbar,
) {
    if (toolbar !== emptyToolbar) {
        ul(classes = "header-button-list") {
            id = "toolbar"
            style = "list-style-type: none;"
            toolbar(this)
        }
    }
    div {
        style = "max-height: 800px; overflow-y: auto; display: block"
        table {
            id = tableId
            attributes["data-toggle"] = "table"
            if (toolbar !== emptyToolbar) {
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
            freezeColumnsStart?.let {
                attributes["data-fixed-columns"] = "true"
                attributes["data-fixed-number"] = it.toString()
            }
            freezeColumnsEnd?.let {
                attributes["data-fixed-right-number"] = it.toString()
            }
            subTableDetails?.let { applySubTabDetails(it) }
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
                addFields(getObjectInstance<T>().tableDisplayFields, clickableRows)
            }
            if (tableButtons.isNotEmpty()) {
                addTableButtons(tableId, tableButtons)
            }
        }
    }
}

/**
 * Constructs a basic [BootstrapTable](https://bootstrap-table.com) that fetches data from the desired [url][dataUrl],
 * displays the [fields] using the keys as reference to JSON data's keys. The [fields] specified also allow the user to
 * add [column options](https://bootstrap-table.com/docs/api/column-options/) as a map of option to value. Optional
 * parameters include [table buttons](https://bootstrap-table.com/docs/api/table-options/#buttons),
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
    customSortFunction: String = "",
    clickableRows: Boolean = true,
    subscriber: String = "",
    subTableDetails: SubTableDetails? = null,
    toolbar: UL.() -> Unit = emptyToolbar,
) {
    if (toolbar !== emptyToolbar) {
        ul(classes = "header-button-list") {
            id = "toolbar"
            style = "list-style-type: none;"
            toolbar(this)
        }
    }
    div {
        style = "max-height: 800px; overflow-y: auto; display: block"
        table {
            id = tableId
            attributes["data-toggle"] = "table"
            if (toolbar !== emptyToolbar) {
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
            if (subTableDetails != null) {
                applySubTabDetails(subTableDetails)
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
                addTableButtons(tableId, tableButtons)
            }
        }
    }
}

