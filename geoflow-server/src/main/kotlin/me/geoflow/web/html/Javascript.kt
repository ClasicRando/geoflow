package me.geoflow.web.html

import kotlinx.html.SCRIPT
import kotlinx.html.unsafe

/** Converts a Kotlin object to it's javascript value that is assigned to a global variable. */
private fun convertToJsValue(value: Any): String {
    return when (value) {
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
        "var $name = ${convertToJsValue(value)}"
    }
    unsafe {
        raw(jsVariables)
    }
}
