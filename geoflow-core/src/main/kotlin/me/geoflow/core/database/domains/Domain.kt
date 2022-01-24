package me.geoflow.core.database.domains

/**
 * Template for defining a Postgresql Domain. Specified the name, data_type and constraints to make a CREATE statement
 */
open class Domain (
    /** non qualified name of the domain */
    val name: String,
    dataType: String,
    vararg constraints: Pair<String, String>,
) {
    /** SQL DDL for creating the specified domain */
    val createStatement: String = """
        CREATE DOMAIN $name AS $dataType ${constraints.joinToString(separator = " ") { (name, expression) ->
            "CONSTRAINT $name CHECK($expression)"
        }}
    """.trimIndent()
}
