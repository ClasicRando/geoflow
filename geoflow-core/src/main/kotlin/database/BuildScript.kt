@file:Suppress("MatchingDeclarationName")
package database

import loading.checkTableExists
import loading.loadDefaultData
import database.functions.Constraints
import database.functions.PlPgSqlFunction
import database.functions.PlPgSqlTableFunction
import database.procedures.SqlProcedure
import database.tables.DbTable
import database.tables.Triggers
import database.tables.DefaultData
import database.tables.DefaultGeneratedData
import database.tables.dataGenerationSql
import database.tables.defaultRecordsFile
import mu.KotlinLogging
import org.reflections.Reflections
import org.reflections.scanners.Scanners.SubTypes
import java.sql.Connection

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

/**
 * Lazy sequence of enums needed for database operations.
 *
 * Uses reflection to find all Enum classes and maps the class to a PostgresEnumType.
 */
private val enums by lazy {
    Reflections("database.enums")
        .get(SubTypes.of(Enum::class.java).asClass<Enum<*>>())
        .asSequence()
        .map { enum ->
            PostgresEnumType(enum.simpleName, enum.enumConstants.map { it.toString() })
        }
}

/** Lazy list of tables declared in the 'orm.tables' package. List items are the Object instances themselves. */
private val tables by lazy {
    Reflections("database.tables")
        .get(SubTypes.of(DbTable::class.java).asClass<DbTable>())
        .asSequence()
        .map { table -> table.getDeclaredField("INSTANCE").get(null)::class }
        .filter { !it.isAbstract }
        .map { kClass -> kClass.objectInstance!! as DbTable }
        .toList()
}

/**
 * Lazy sequence of procedures declared in the 'database.procedures' package. List items are the Object instances
 * themselves.
 */
private val procedures by lazy {
    Reflections("database.procedures")
        .get(SubTypes.of(SqlProcedure::class.java).asClass<SqlProcedure>())
        .asSequence()
        .map { procedure -> procedure.getDeclaredField("INSTANCE").get(null)::class }
        .filter { !it.isAbstract }
        .map { kClass -> kClass.objectInstance!! as SqlProcedure }
}

/**
 * Lazy sequence of table functions declared in the 'database.functions' package. List items are the Object instances
 * themselves.
 */
private val tableFunctions by lazy {
    Reflections("database.functions")
        .get(SubTypes.of(PlPgSqlTableFunction::class.java).asClass<PlPgSqlTableFunction>())
        .asSequence()
        .map { procedure -> procedure.getDeclaredField("INSTANCE").get(null)::class }
        .filter { !it.isAbstract }
        .map { kClass -> kClass.objectInstance!! as PlPgSqlTableFunction }
}

/**
 * Lazy sequence of table functions declared in the 'database.functions' package. List items are the Object instances
 * themselves.
 */
private val functions by lazy {
    Reflections("database.functions")
        .get(SubTypes.of(PlPgSqlFunction::class.java).asClass<PlPgSqlFunction>())
        .asSequence()
        .map { procedure -> procedure.getDeclaredField("INSTANCE").get(null)::class }
        .filter { !it.isAbstract }
        .map { kClass -> kClass.objectInstance!! as PlPgSqlFunction }
}

private val logger = KotlinLogging.logger {}

/**
 * Extension function to create a given [table] instance within the current Connection. Multiple steps might be required
 * depending upon the complexity of the table definition. Steps may include:
 * 1. Execute the CREATE TABLE statement stored in [createStatement][DbTable.createStatement] property.
 * 2. If the table has triggers (ie table extends interface [Triggers]), loop through list of [database.tables.Trigger]
 * data classes to create the trigger function then run the CREATE TRIGGER statement.
 * 3. If the table had initial data to load (ie table extends interface [DefaultData]), get the resource's
 * [InputStream][java.io.InputStream] to COPY the file to the current table. When the load is done, set the sequence
 * value to the current max value in the table if the table has a sequential primary key.
 */
private fun Connection.createTable(table: DbTable) {
    logger.info("Starting ${table.tableName}")
    require(!checkTableExists(table.tableName)) { "${table.tableName} already exists" }
    logger.info("Creating ${table.tableName}")
    this.prepareStatement(table.createStatement).execute()
    if (table is Triggers) {
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
    if (table is DefaultData) {
        table.defaultRecordsFile?.let { defaultRecordsStream ->
            val recordCount = loadDefaultData(table.tableName, defaultRecordsStream)
            logger.info("Inserted $recordCount records into ${table.tableName}")
        }
    }
    if (table is DefaultGeneratedData) {
        table.dataGenerationSql?.let { sqlFileStream ->
            val sqlText = sqlFileStream.bufferedReader().use { it.readText() }
            prepareCall(sqlText).use { statement ->
                statement.execute()
            }
            logger.info("Inserted records into ${table.tableName}")
        }
    }
}

/**
 * Recursive function that creates all tables initially passed, accounting for foreign key dependencies.
 *
 * Initial state of the database is no tables created and a full list of [tables] to create. The first iteration works
 * with all tables that do not have foreign key constraints since they can be created easily without complex
 * dependencies. Once those tables are created, the function calls itself with a new list of tables to create (the
 * passed list with tables created in this iteration removed) and a running [set of tables][createdTables] already
 * created. All iterations after this point, create tables that have foreign keys and all the dependencies can be found
 * in the set of already created tables, and each time a table is created, it is added to running set of tables created.
 * The process exits the recursion when the passed list of tables to create is empty.
 */
private fun Connection.createTables(
    tables: List<DbTable>,
    createdTables: Set<String> = emptySet()
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
    createTables(
        tables.minus(tablesToCreate.toSet()),
        createdTables.union(tablesToCreate.map { it.tableName }),
    )
}

/** Create all enums  */
private fun Connection.createEnums() {
    for (enum in enums) {
        logger.info("Creating ${enum.postgresName}")
        prepareStatement(enum.create).use {
            it.execute()
        }
    }
}

/** Create all constraint functions */
private fun Connection.createConstraintFunctions() {
    for (constraint in Constraints.functions) {
        val functionName = "FUNCTION (public\\.)?(.+) ".toRegex()
            .find(constraint)
            ?.groupValues
            ?.get(2)
        logger.info("Creating constraint function ${functionName ?: "!! NAME UNKNOWN !!\n$constraint"}")
        prepareStatement(constraint).use {
            it.execute()
        }
    }
}

/** Create all procedures */
private fun Connection.createProcedures() {
    for (procedure in procedures) {
        logger.info("Creating ${procedure.name}")
        prepareStatement(procedure.code).use {
            it.execute()
        }
    }
}

/** Create all functions */
private fun Connection.createFunctions() {
    for (tableFunction in functions) {
        logger.info("Creating ${tableFunction.name}")
        for (innerFunction in tableFunction.innerFunctions) {
            prepareStatement(innerFunction).use {
                it.execute()
            }
        }
        prepareStatement(tableFunction.functionCode).use {
            it.execute()
        }
    }
}

/** Create all table functions */
private fun Connection.createTableFunctions() {
    for (tableFunction in tableFunctions) {
        logger.info("Creating ${tableFunction.name}")
        for (innerFunction in tableFunction.innerFunctions) {
            prepareStatement(innerFunction).use {
                it.execute()
            }
        }
        prepareStatement(tableFunction.functionCode).use {
            it.execute()
        }
    }
}

/**
 * Uses the database connection to create all required objects, tables, procedures, and table functions for application.
 */
@Suppress("UNUSED")
fun Connection.buildDatabase() {
    @Suppress("TooGenericExceptionCaught")
    try {
        createEnums()
        createConstraintFunctions()
        createTables(tables)
        createProcedures()
        createFunctions()
        createTableFunctions()
    } catch (t: Throwable) {
        logger.error("Error trying to construct database schema", t)
    } finally {
        logger.info("Exiting DB build")
    }
}

/** Entry point to building the database */
fun main() {
    Database.runWithConnectionBlocking {
        it.buildDatabase()
    }
}
