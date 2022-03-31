package me.geoflow.core.database.composites

/** */
data class CompositeDefinition(
    /** */
    val name: String,
    /** */
    val createStatement: String,
    /** */
    val subComposites: Set<String>,
)
