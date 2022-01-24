package me.geoflow.core.web.html

import kotlinx.html.SCRIPT
import kotlinx.html.unsafe

/** Inline class for creating a JQuery variable*/
@JvmInline
@Suppress("MatchingDeclarationName")
value class JQuery(
    /** query used to initialize the variable */
    val query: String,
)

/** Inline class for creating an Element variable */
@JvmInline
@Suppress("MatchingDeclarationName")
value class QuerySelector(
    /** query used to initialize the variable */
    val query: String,
)

/**
 * Container to create an element for a javascript variable using a css selector query
 *
 * When passed with a variable name to the [addParamsAsJsGlobalVariables] function, the class will attempt to create
 * 2 variables:
 * - Element object (using the name directly)
 * - JQuery object (prepends the name with '$')
 *
 * These variables will be accessible to the global scope and will be the result or running the query provided. To avoid
 * creating 1 of the 2 variables, override the appropriate `make` parameter to false. This can be helpful if the JQuery
 * or Element variable would not be used. For example, if the variable queries a basic table, it's less likely you will
 * need to have the Element variable since you will be mostly interfacing with the table through the bootstrapTable
 * JQuery function.
 */
data class JSElement(
    /** query used to initialize the variable */
    val query: String,
    /** flag indicating a jquery object should be made */
    val makeJQuery: Boolean = true,
    /** flag indicating an element object should be made */
    val makeSelector: Boolean = true,
)

/** Converts a Kotlin object to it's javascript value that is assigned to a global variable. */
private fun convertToJsValue(value: Any): String {
    return when (value) {
        is JQuery -> "$('${value.query}')"
        is QuerySelector -> "document.querySelector('${value.query}')"
        is String -> "'${value.replace("'", "\\'")}'"
        is Number -> value.toString()
        is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}") {
            "${it.key}: ${convertToJsValue(it.value ?: "")}"
        }
        is Array<*> -> value.joinToString(separator = ",", prefix = "[", postfix = "]") {
            convertToJsValue(it ?: "")
        }
        is Iterable<*> -> value.joinToString(separator = ",", prefix = "[", postfix = "]") {
            convertToJsValue(it ?: "")
        }
        else -> "'$value'"
    }
}

/**
 * Extension function for the script tag that accept a [Map] of [params] that represent a javascript variable name and
 * the variable value as converted from Kotlin object to a javascript object.
 */
fun SCRIPT.addParamsAsJsGlobalVariables(vararg params: Pair<String, Any>) {
    val jsVariables = params.joinToString(separator = ";", postfix = ";") { (name, value) ->
        if (value is JSElement) {
            val jquery = if (value.makeJQuery) "var \$$name = ${convertToJsValue(JQuery(value.query))}" else ""
            val selector = if (value.makeSelector) "var $name = ${convertToJsValue(QuerySelector(value.query))}" else ""
            "$selector;$jquery".trim(';')
        } else {
            "var $name = ${convertToJsValue(value)}"
        }
    }
    unsafe {
        raw(jsVariables)
    }
}
