package database

import data_loader.checkTableExists
import data_loader.loadDefaultData
import mu.KotlinLogging
import org.reflections.Reflections
import org.reflections.scanners.Scanners.*
import orm.tables.*
import java.sql.Connection

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

val tableInterfaces by lazy {
    Reflections("orm.tables")
        .get(SubTypes.of(TableBuildRequirement::class.java))
        .map { className -> ClassLoader.getSystemClassLoader().loadClass(className) }
}

private val enums by lazy {
    Reflections("orm.enums")
        .get(SubTypes.of(Enum::class.java).asClass<Enum<*>>())
        .asSequence()
        .map { enum ->
            PostgresEnumType(enum.simpleName, enum.enumConstants.map { it.toString() })
        }
}

val tables by lazy {
    Reflections("orm.tables")
        .get(SubTypes.of(DbTable::class.java).asClass<DbTable<*>>())
        .asSequence()
        .map { table -> table.getDeclaredField("INSTANCE").get(null)::class }
        .filter { !it.isAbstract }
        .map { kClass -> kClass.objectInstance!! as DbTable<*> }
        .toList()
}

private val logger = KotlinLogging.logger {}

private fun Connection.createTable(table: DbTable<*>) {
    logger.info("Starting ${table.tableName}")
    require(!checkTableExists(table.tableName)) { "${table.tableName} already exists" }
    val interfaces = table::class.java.interfaces.filter { it in tableInterfaces }
    val sequentialPrimaryKey = SequentialPrimaryKey::class.java in interfaces
    var sequenceName = ""
    var pkField = ""
    if (sequentialPrimaryKey) {
        pkField = "PRIMARY KEY \\((.+)\\)".toRegex()
            .find(table.createStatement)
            ?.groupValues
            ?.get(1)
            ?: throw IllegalStateException("Cannot find primary key constraint for sequential primary key interface")
        val pkFieldMatch = "$pkField ([a-z]+) .+ nextval\\('(.+)'::regclass\\)".toRegex()
            .find(table.createStatement)
            ?: throw IllegalStateException("Cannot find sequence name for sequential primary key interface")
        val maxValue = when(pkFieldMatch.groupValues[1]) {
            "integer" -> 2147483647
            "bigint" -> 9223372036854775807
            else -> throw IllegalStateException("PK field type must be a numeric serial type")
        }
        sequenceName = pkFieldMatch.groupValues[2]
        logger.info("Creating ${table.tableName}'s sequence, $sequenceName")
        prepareStatement("""
            CREATE SEQUENCE public.$sequenceName
                INCREMENT 1
                START 1
                MINVALUE 1
                MAXVALUE $maxValue
                CACHE 1;
        """.trimIndent())
            .use {
                it.execute()
            }
    }
    logger.info("Creating ${table.tableName}")
    this.prepareStatement(table.createStatement).execute()
    if (Triggers::class.java in interfaces) {
        (table as Triggers).triggers.forEach { trigger ->
            val functionName = "EXECUTE FUNCTION (public\\.)?(.+)\\(\\)"
                .toRegex()
                .find(trigger.trigger)
                ?.groupValues
                ?.get(2)
                ?: throw IllegalStateException("Cannot find trigger function name")
            logger.info("Creating ${table.tableName}'s trigger function, $functionName")
            this.prepareStatement(trigger.triggerFunction).execute()
            val triggerName = "CREATE TRIGGER (\\S+)"
                .toRegex()
                .find(trigger.trigger)
                ?.groupValues
                ?.get(1)
                ?: throw IllegalStateException("Cannot find trigger name")
            logger.info("Creating ${table.tableName}'s trigger, $triggerName")
            this.prepareStatement(trigger.trigger)
        }
    }
    if (DefaultData::class.java in interfaces) {
        (table as DefaultData).defaultRecordsFile?.let { defaultRecordsStream ->
            val recordCount = loadDefaultData(table.tableName, defaultRecordsStream)
            logger.info("Inserted $recordCount records into ${table.tableName}")
        }
        if (sequentialPrimaryKey) {
            this.prepareStatement("SELECT setval(?, max($pkField)) from ${table.tableName}").apply {
                setString(1, sequenceName)
            }.use {
                it.execute()
            }
        }
    }
}

private fun Connection.createTables(
    tables: List<DbTable<*>>,
    createdTables: Set<String> = setOf()
) {
    if (tables.isEmpty()) {
        return
    }
    val tablesToCreate = if (createdTables.isEmpty()) {
        tables.filter { !it.hasForeignKey }
    } else {
        tables.filter { table -> table.referencedTables.all { it in createdTables } }
    }
    for (table in tablesToCreate) {
        createTable(table)
    }
    createTables(tables.minus(tablesToCreate), createdTables.union(tablesToCreate.map { it.tableName }))
}

fun buildDatabase() {
    try {
        DatabaseConnection.database.useConnection { connection ->
            for (enum in enums) {
                logger.info("Creating ${enum.postgresName}")
                connection.prepareStatement(enum.create).execute()
            }
            connection.createTables(tables)
        }
    } catch (ex: Exception) {
        logger.error("Error trying to construct database schema", ex)
    } finally {
        logger.info("Exiting DB build")
    }
}
