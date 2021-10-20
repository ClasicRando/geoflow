package html

import kotlinx.html.SCRIPT
import kotlinx.html.unsafe

private fun convertToJsValue(value: Any): String {
    return when(value) {
        is String -> "'${value.replace("'", "\\'")}'"
        is Number -> value.toString()
        is Map<*, *> -> value.map {
            "${it.key}: ${convertToJsValue(it.value ?: "")}"
        }.joinToString(prefix = "{", postfix = "}")
        is Array<*> -> value.joinToString(separator = ",", prefix = "[", postfix = "]") {
            convertToJsValue(it ?: "")
        }
        is Iterable<*> -> value.joinToString(separator = ",", prefix = "[", postfix = "]") {
            convertToJsValue(it ?: "")
        }
        else -> "'$value'"
    }
}

fun SCRIPT.addParamsAsJsGlobalVariables(params: Map<String, Any>) {
    val jsVariables = params
        .mapValues { (_, value) -> convertToJsValue(value) }
        .map{ (name, value) -> "var $name = $value" }
        .joinToString(separator = ";", postfix = ";")
    unsafe {
        raw(jsVariables)
    }
}