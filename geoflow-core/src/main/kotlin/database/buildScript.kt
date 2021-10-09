package database

import mu.KotlinLogging
import org.reflections.Reflections
import org.reflections.scanners.Scanners.*

data class PostgresEnumType(val name: String, val constantValues: List<String>) {
    val postgresName = name.replace("[A-Z]".toRegex()) { "_" + it.value.lowercase() }.trimStart('_')
    val create = """
        CREATE TYPE public.$postgresName AS ENUM(${
            this.constantValues.joinToString(
                separator = "','",
                prefix = "'",
                postfix = "'"
            )
        });
    """.trimIndent()
}

val logger = KotlinLogging.logger {}

fun buildDatabase() {
    try {
        DatabaseConnection.database.useConnection { connection ->
            val enums = Reflections("orm.enums")
                .get(SubTypes.of(Enum::class.java).asClass<Enum<*>>())
                .map { enum ->
                    PostgresEnumType(enum.simpleName, enum.enumConstants.map { it.toString() })
                }
            val dbEnums = connection
                .prepareStatement(
                    """
                    SELECT t1.typname, array_agg(t2.enumlabel)
                    FROM   pg_catalog.pg_type t1
                    LEFT JOIN pg_catalog.pg_enum t2
                    ON     t1.oid = t2.enumtypid
                    WHERE  typname in (${Array(enums.size) { "?" }.joinToString()})
                    AND    typtype = 'e'
                    GROUP BY t1.typname
                    """.trimIndent()
                )
                .apply {
                    enums.forEachIndexed { index, enum ->
                        setString(index + 1, enum.postgresName)
                    }
                }
                .executeQuery()
                .use { rs ->
                    generateSequence {
                        if (rs.next()) Pair(rs.getString(1), rs.getArray(2).array) else null
                    }.toList()
                }
            if (dbEnums.isEmpty()) {
                logger.info("No conflicting Enum types found")
            } else {
                logger.info("Conflicting Enum types found: ${dbEnums.joinToString { it.first }}")
                for (enum in dbEnums) {
                    val requiredValues = enums
                        .first { it.postgresName == enum.first }
                        .constantValues
                    val currentValues = (enum.second as Array<*>)
                        .map { it as String }
                    val missingValues = requiredValues.minus(currentValues)
                    val extraValues = currentValues.minus(requiredValues)
                    when {
                        missingValues.isNotEmpty() ->
                            throw IllegalStateException("${enum.first}: missing values, ${missingValues.joinToString()}")
                        extraValues.isNotEmpty() ->
                            throw IllegalStateException("${enum.first}: extra values, ${extraValues.joinToString()}")
                        else -> logger.info("${enum.first}: values match definitions")
                    }
                }
            }
        }
    } catch (ex: Exception) {
        logger.error("Error trying to construct database schema", ex)
    }
}