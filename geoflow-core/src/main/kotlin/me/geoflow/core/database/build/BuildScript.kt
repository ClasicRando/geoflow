package me.geoflow.core.database.build

import me.geoflow.core.database.extensions.executeNoReturn
import me.geoflow.core.database.functions.Constraints
import me.geoflow.core.database.functions.PlPgSqlFunction
import me.geoflow.core.database.functions.PlPgSqlTableFunction
import me.geoflow.core.database.procedures.SqlProcedure
import me.geoflow.core.database.tables.DbTable
import me.geoflow.core.database.tables.DefaultData
import me.geoflow.core.database.tables.DefaultGeneratedData
import me.geoflow.core.database.tables.Triggers
import me.geoflow.core.database.tables.dataGenerationSql
import me.geoflow.core.database.tables.defaultRecordsFile
import me.geoflow.core.loading.checkTableExists
import me.geoflow.core.loading.loadDefaultData
import mu.KotlinLogging
import org.reflections.Reflections
import org.reflections.scanners.Scanners.SubTypes
import java.sql.Connection

/** */
object BuildScript {

    private val logger = KotlinLogging.logger {}

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
     * Lazy sequence of table functions declared in the 'database.functions' package. List items are the Object
     * instances themselves.
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
     * Lazy sequence of table functions declared in the 'database.functions' package. List items are the Object
     * instances themselves.
     */
    private val functions by lazy {
        Reflections("database.functions")
            .get(SubTypes.of(PlPgSqlFunction::class.java).asClass<PlPgSqlFunction>())
            .asSequence()
            .map { procedure -> procedure.getDeclaredField("INSTANCE").get(null)::class }
            .filter { !it.isAbstract }
            .map { kClass -> kClass.objectInstance!! as PlPgSqlFunction }
    }

    /** */
    private fun createTriggers(connection: Connection, table: DbTable) = with(connection) {
        require(table is Triggers)
        for (trigger in table.triggers) {
            val functionName = "EXECUTE FUNCTION (public\\.)?(.+)\\(\\)"
                .toRegex()
                .find(trigger.trigger)
                ?.groupValues
                ?.get(2)
                ?: throw IllegalStateException("Cannot find trigger function name")
            logger.info("Creating ${table.tableName}'s trigger function, $functionName")
            executeNoReturn(trigger.triggerFunction)
            val triggerName = "CREATE TRIGGER (\\S+)"
                .toRegex()
                .find(trigger.trigger)
                ?.groupValues
                ?.get(1)
                ?: throw IllegalStateException("Cannot find trigger name")
            logger.info("Creating ${table.tableName}'s trigger, $triggerName")
            executeNoReturn(trigger.trigger)
        }
    }

    /**
     * Extension function to create a given [table] instance within the current Connection. Multiple steps might be
     * required depending upon the complexity of the table definition. Steps may include:
     * 1. Execute the CREATE TABLE statement stored in [createStatement][DbTable.createStatement] property.
     * 2. If the table has triggers (ie table extends interface [Triggers]), loop through list of
     * [me.geoflow.core.database.tables.Trigger] data classes to create the trigger function then run the CREATE TRIGGER
     * statement.
     * 3. If the table had initial data to load (ie table extends interface [DefaultData]), get the resource's
     * [InputStream][java.io.InputStream] to COPY the file to the current table. When the load is done, set the sequence
     * value to the current max value in the table if the table has a sequential primary key.
     */
    private fun createTable(connection: Connection, table: DbTable) {
        logger.info("Starting ${table.tableName}")
        require(!connection.checkTableExists(table.tableName)) { "${table.tableName} already exists" }
        logger.info("Creating ${table.tableName}")
        connection.executeNoReturn(table.createStatement)
        if (table is Triggers) {
            createTriggers(connection, table)
        }
        if (table is DefaultData) {
            table.defaultRecordsFile?.let { defaultRecordsStream ->
                val recordCount = connection.loadDefaultData(table.tableName, defaultRecordsStream)
                logger.info("Inserted $recordCount records into ${table.tableName}")
            }
        }
        if (table is DefaultGeneratedData) {
            table.dataGenerationSql?.let { sqlFileStream ->
                val sqlText = sqlFileStream.bufferedReader().use { it.readText() }
                connection.executeNoReturn(sqlText)
                logger.info("Inserted records into ${table.tableName}")
            }
        }
    }

    /**
     * Recursive function that creates all tables initially passed, accounting for foreign key dependencies.
     *
     * Initial state of the database is no tables created and a full list of [tables] to create. The first iteration
     * works with all tables that do not have foreign key constraints since they can be created easily without complex
     * dependencies. Once those tables are created, the function calls itself with a new list of tables to create (the
     * passed list with tables created in this iteration removed) and a running [set of tables][createdTables] already
     * created. All iterations after this point, create tables that have foreign keys and all the dependencies can be
     * found in the set of already created tables, and each time a table is created, it is added to running set of
     * tables created. The process exits the recursion when the passed list of tables to create is empty.
     */
    private fun createTables(
        connection: Connection,
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
            createTable(connection, table)
        }
        createTables(
            connection,
            tables.minus(tablesToCreate.toSet()),
            createdTables.union(tablesToCreate.map { it.tableName }),
        )
    }

    /** Create all enums  */
    private fun createEnums(connection: Connection) {
        for (enum in enums) {
            logger.info("Creating ${enum.postgresName}")
            connection.executeNoReturn(enum.create)
        }
    }

    /** Create all constraint functions */
    private fun createConstraintFunctions(connection: Connection) {
        for (constraint in Constraints.functions) {
            val functionName = "FUNCTION (public\\.)?(.+) ".toRegex()
                .find(constraint)
                ?.groupValues
                ?.get(2)
            logger.info("Creating constraint function ${functionName ?: "!! NAME UNKNOWN !!\n$constraint"}")
            connection.executeNoReturn(constraint)
        }
    }

    /** Create all procedures */
    private fun createProcedures(connection: Connection) {
        for (procedure in procedures) {
            logger.info("Creating ${procedure.name}")
            connection.executeNoReturn(procedure.code)
        }
    }

    /** Create all functions */
    private fun createFunctions(connection: Connection) {
        for (tableFunction in functions) {
            logger.info("Creating ${tableFunction.name}")
            for (innerFunction in tableFunction.innerFunctions) {
                connection.executeNoReturn(innerFunction)
            }
            connection.executeNoReturn(tableFunction.functionCode)
        }
    }

    /** Create all table functions */
    private fun createTableFunctions(connection: Connection) {
        for (tableFunction in tableFunctions) {
            logger.info("Creating ${tableFunction.name}")
            for (innerFunction in tableFunction.innerFunctions) {
                connection.executeNoReturn(innerFunction)
            }
            connection.executeNoReturn(tableFunction.functionCode)
        }
    }

    /**
     * Uses the database connection to create all required objects, tables, procedures, and table functions for
     * use within the application.
     */
    fun buildDatabase(connection: Connection) {
        @Suppress("TooGenericExceptionCaught")
        try {
            createEnums(connection)
            createConstraintFunctions(connection)
            createTables(connection, tables)
            createProcedures(connection)
            createFunctions(connection)
            createTableFunctions(connection)
        } catch (t: Throwable) {
            logger.error("Error trying to construct database schema", t)
        } finally {
            logger.info("Exiting DB build")
        }
    }

}
