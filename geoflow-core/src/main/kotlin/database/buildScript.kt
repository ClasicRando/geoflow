package database

import mu.KotlinLogging
import org.reflections.Reflections
import org.reflections.scanners.Scanners.*
import orm.tables.DbTable
import java.sql.Connection
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

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

fun Connection.createTable(table: DbTable<*>) {
    logger.info("Starting ${table.tableName}")
    val fields = table::class.memberProperties
        .filter { it.returnType.jvmErasure == String::class }
        .associate { property ->
            val value = property
                .getter
                .call(table)
                ?.toString()
                ?: ""
            Pair(property.name, value)
        }
    fields["createSequence"]?.let { createSequence ->
        val sequenceName = "CREATE SEQUENCE public\\.(\\S+)"
            .toRegex()
            .find(createSequence)
            ?.groupValues
            ?.get(1)
            ?: ""
        logger.info("Creating ${table.tableName}'s sequence, $sequenceName")
        this.prepareStatement(createSequence).execute()
    }
    this
        .prepareStatement("SELECT * FROM information_schema.tables WHERE table_name = ?")
        .apply {
            setString(1, table.tableName)
        }
        .executeQuery()
        .use { rs ->
            if (rs.next())
                throw IllegalStateException("${table.tableName} already exists")
        }
    logger.info("Creating ${table.tableName}")
    this.prepareStatement(table.createStatement).execute()
    fields
        .filterValues { it.startsWith("CREATE TRIGGER", ignoreCase = true) }
        .forEach { (_, statement) ->
            val functionName = "EXECUTE FUNCTION (public\\.)?(.+)\\(\\)"
                .toRegex()
                .find(statement)
                ?.groupValues
                ?.get(2) ?: throw IllegalStateException("Cannot find trigger function name")
            val functionStatement = fields
                .filterValues { it.contains(functionName) }
                .firstNotNullOfOrNull { it.value }
                ?: throw IllegalStateException("No property to match the trigger function name")
            logger.info("Creating ${table.tableName}'s trigger function, $functionName")
            this.prepareStatement(functionStatement).execute()
            val triggerName = "CREATE TRIGGER (\\S+)"
                .toRegex()
                .find(statement)
                ?.groupValues
                ?.get(1) ?: throw IllegalStateException("Cannot find trigger name")
            logger.info("Creating ${table.tableName}'s trigger, $triggerName")
            this.prepareStatement(statement)
        }
}

fun Connection.createTables(tables: List<DbTable<*>>, createdTables: Set<String> = setOf()) {
    if (tables.isEmpty()) {
        return
    }
    val tablesToCreate = if (createdTables.isEmpty()) {
        tables.filter { !it.hasForeignKey }
    } else {
        tables.filter { table -> table.hasForeignKey && table.referencedTables.any { it in createdTables } }
    }
    for (table in tablesToCreate) {
        this.createTable(table)
    }
    this.createTables(tables.minus(tablesToCreate), createdTables.union(tablesToCreate.map { it.tableName }))
}

fun buildDatabase() {
    try {
        DatabaseConnection.database.useConnection { connection ->
            Reflections("orm.enums")
                .get(SubTypes.of(Enum::class.java).asClass<Enum<*>>())
                .asSequence()
                .map { enum ->
                    PostgresEnumType(enum.simpleName, enum.enumConstants.map { it.toString() })
                }
                .forEach { enum ->
                    logger.info("Creating ${enum.postgresName}")
                    connection.prepareStatement(enum.create).execute()
                }
            val tables = Reflections("orm.tables")
                .get(SubTypes.of(DbTable::class.java).asClass<DbTable<*>>())
                .asSequence()
                .map { table -> table.getDeclaredField("INSTANCE").get(null)::class }
                .filter { !it.isAbstract }
                .map { kClass -> kClass.objectInstance!! as DbTable<*> }
                .toList()
            connection.createTables(tables)
        }
    } catch (ex: Exception) {
        logger.error("Error trying to construct database schema", ex)
    } finally {
        logger.info("Exiting DB build")
    }
}