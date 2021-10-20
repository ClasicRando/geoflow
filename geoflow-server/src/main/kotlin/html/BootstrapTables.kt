package html

import kotlinx.html.*

fun getFieldTable(field: String): String = field
    .replace("_", " ")
    .fold(StringBuilder()) { acc, c ->
        acc.append(if (acc.ifEmpty { " " }.last().isWhitespace()) c.titlecase() else c)
    }
    .toString()

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

data class HeaderButton(val name: String, val html: UL.() -> Unit)

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

fun FlowContent.autoRefreshTable(
    tableId: String,
    dataUrl: String,
    fields: Map<String, Map<String, String>>,
    tableButtons: List<TableButton> = listOf(),
    headerButtons: List<HeaderButton> = listOf(),
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
        attributes["data-auto-refresh"] = "true"
        attributes["data-auto-refresh-interval"] = "1"
        attributes["data-auto-refresh-status"] = "true"
        attributes["data-classes"] = "table table-bordered"
        attributes["data-thead-classes"] = "thead-dark"
        attributes["data-search"] = "true"
        if (tableButtons.isNotEmpty()) {
            attributes["data-buttons"] = "${tableId}buttons"
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