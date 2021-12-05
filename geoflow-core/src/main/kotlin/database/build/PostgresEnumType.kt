package database.build

/** Container for an enum translation to a PostgreSQL enum creation */
data class PostgresEnumType(
    /** name of enum type */
    val name: String,
    /** list of values the enum should contain */
    val constantValues: List<String>,
) {
    /** converts the Kotlin enum type name to the desired postgresql enum name */
    val postgresName: String = name.replace("[A-Z]".toRegex()) { "_" + it.value.lowercase() }.trimStart('_')
    /** create statement of the enum */
    val create: String = """
        CREATE TYPE public.$postgresName AS ENUM(${
            this.constantValues.joinToString(
                separator = "','",
                prefix = "'",
                postfix = "'"
            )
        });
    """.trimIndent()
}
