package html

import kotlinx.html.*

fun getFieldTable(field: String): String = field.fold(StringBuilder()) { acc, c ->
    acc.append(if (acc.ifEmpty { " " }.last().isWhitespace()) c.titlecase() else c)
}.toString()

fun FlowContent.basicTable(
    tableName: String,
    tableId: String,
    dataUrl: String,
    fields: Map<String, Map<String, String>>
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
    }
}