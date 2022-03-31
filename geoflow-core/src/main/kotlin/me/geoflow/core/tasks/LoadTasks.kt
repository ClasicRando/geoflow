@file:Suppress("UNUSED")
package me.geoflow.core.tasks

import me.geoflow.core.loading.AnalyzeResult
import me.geoflow.core.loading.analyzeFile
import me.geoflow.core.loading.loadFile
import me.geoflow.core.database.tables.PipelineRuns
import me.geoflow.core.database.tables.SourceTableColumns
import me.geoflow.core.database.tables.SourceTables
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import me.geoflow.core.database.extensions.executeNoReturn
import me.geoflow.core.database.extensions.runUpdate
import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.tables.PipelineRunTasks
import me.geoflow.core.database.tables.PlottingFields
import me.geoflow.core.database.tables.PlottingMethods
import me.geoflow.core.database.tables.records.PipelineRunTask
import me.geoflow.core.web.html.SubTableDetails
import me.geoflow.core.web.html.basicTable
import java.io.File
import java.sql.Connection
import java.time.Instant
import java.time.ZoneId

/**
 * User task to notify the user that the record date of the collected data is outside the threshold for relevant data
 */
@UserTask(taskName = "Recollect Data")
const val RECOLLECT_DATA: Long = 16L

/**
 * User task with a modal window showing table stats collected by the parent task
 */
@UserTask(taskName = "Table Stats (INFO)")
const val TABLE_STATS: Long = 18L

/**
 * User task when the data source has not been loaded before so no comparison is required
 */
@UserTask(taskName = "No Table Stats Comparison Required")
const val NO_COMPARISON_REQUIRED: Long = 19L

/**
 * User task when the data source has not been loaded before so plotting fields must be set manually
 */
@UserTask(taskName = "Manually Set Plotting Fields")
const val MANUALLY_SET_PLOTTING_FIELDS: Long = 21L

/**
 * User task when the data source has not been loaded before so plotting methods must be set manually
 */
@UserTask(taskName = "Manually Set Plotting Methods")
const val MANUALLY_SET_PLOTTING_METHODS: Long = 24L

/**
 * System task to analyze all the source files for a pipeline run that are marked to be analyzed.
 *
 * Iterates over all the source tables in a pipeline run that have a 'true' analyze field and analyze the specified
 * file. One file might have multiple sub tables so each source table record is grouped by filename. After the files
 * have been analyzed, the column stats are inserted (or updated if they already exist) into the [SourceTableColumns]
 * table and the [SourceTables] record is updated to show it has been analyzed.
 */
@SystemTask(taskId = 12, taskName = "Analyze Files")
suspend fun analyzeFiles(connection: Connection, prTask: PipelineRunTask) {
    val pipelineRun = PipelineRuns.getRun(connection, prTask.runId)
    val results = mutableListOf<AnalyzeResult>()
    for (fileInfo in SourceTables.filesToAnalyze(connection, pipelineRun.runId)) {
        val file = File(pipelineRun.runFilesLocation, fileInfo.fileName)
        analyzeFile(
            file = file,
            analyzers = fileInfo.analyzeInfo,
        ).buffer().flowOn(Dispatchers.IO).collect { analyzeResult ->
            results += analyzeResult
        }
    }
    if (results.isNotEmpty()) {
        SourceTables.finishAnalyze(connection, results)
    }
}

/**
 * System task to load all the source files for a pipeline run that are marked to be loaded.
 *
 * Iterates over all the source tables in a pipeline run that have a 'true' load field and load the specified file. One
 * file might have multiple sub tables so each source table record is grouped by filename. After the files have been
 * loaded, the [SourceTables] record is updated to show it has been loaded.
 */
@SystemTask(taskId = 13, taskName = "Load Files")
suspend fun loadFiles(connection: Connection, prTask: PipelineRunTask) {
    val pipelineRun = PipelineRuns.getRun(connection, prTask.runId)
    for (file in SourceTables.filesToLoad(connection, prTask.runId)) {
        for (loadingInfo in file.loaders) {
            connection.executeNoReturn("DROP TABLE IF EXISTS ${loadingInfo.tableName}")
            connection.executeNoReturn(loadingInfo.createStatement)
        }
        connection.loadFile(
            file = File(pipelineRun.runFilesLocation, file.fileName),
            loaders = file.loaders,
        )
        connection.runUpdate(
            sql = """
                UPDATE ${SourceTables.tableName}
                SET    load = false
                WHERE  st_oid IN (${"?,".repeat(file.loaders.size).trim(',')})
            """.trimIndent(),
            file.loaders.map { it.stOid },
        )
    }
}

/**
 * System task to look up the last run's source tables and rename the tables with the postfix of '_old'. If another
 * table is already named with '_old' that table will be dropped since those tables are from 2 runs prior.
 */
@SystemTask(taskId = 14, taskName = "Backup Old Tables")
fun backupOldTables(connection: Connection, prTask: PipelineRunTask): String {
    val lastRunId = PipelineRuns.lastRun(connection, prTask.runId)
        ?: return "This is the first run for the data source so no need to backup previous load tables"
    val tableNames = connection.submitQuery<String>(
        sql = """
            SELECT lower(table_name)
            FROM   ${SourceTables.tableName}
            WHERE  run_id = ?
        """.trimIndent(),
        lastRunId
    )
    for (table in tableNames) {
        connection.executeNoReturn("DROP TABLE IF EXISTS ${table}_old")
        connection.executeNoReturn("ALTER TABLE IF EXISTS $table RENAME TO ${table}_old")
    }
    return "Backed up: ${tableNames.joinToString(separator = "','", prefix = "'", postfix = "'")}"
}

/** Threshold for how many days old a record date can be before the data is deemed to be old */
private const val DAYS_OLD = 14L

/**
 * System task to check if the record date of the collected data is outside the threshold for relevant data. Spawns
 * User tasks to alert the user that the data is old.
 */
@SystemTask(taskId = 15, taskName = "Check If Data Is Old")
fun checkIfDataIsOld(connection: Connection, prTask: PipelineRunTask) {
    val pipelineRun = PipelineRuns.getRun(connection, prTask.runId)
    val currentDay = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
    if (pipelineRun.recordLocalDate.plusDays(DAYS_OLD).isBefore(currentDay)) {
        PipelineRunTasks.addTask(connection, prTask.pipelineRunTaskId, RECOLLECT_DATA)
    }
}

/**
 * System task to check if the record date of the collected data is outside the threshold for relevant data. Spawns
 * User tasks to alert the user that the data is old.
 */
@SystemTask(taskId = 17, taskName = "Check Table Stats")
fun checkTableStats(connection: Connection, prTask: PipelineRunTask) {
    val lastRun = PipelineRuns.lastRun(connection, prTask.runId)
    if (lastRun == null) {
        PipelineRunTasks.addTask(connection, prTask.pipelineRunTaskId, NO_COMPARISON_REQUIRED)
        return
    }
    PipelineRunTasks.addTask(connection, prTask.pipelineRunTaskId, TABLE_STATS) {
        basicTable(
            tableId = "tableCounts",
            fields = SourceTables.tableCountComparisonFields,
            dataUrl = "/source-tables/comparisons/${prTask.runId}",
            dataField = "payload",
            subTableDetails = SubTableDetails(
                url = "/source-table-columns/comparison/{id}",
                idField = "st_oid",
                fields = SourceTableColumns.columnComparisonFields,
            ),
            clickableRows = false,
        )
    }
}

/**
 * System task to bring forward past plotting fields by linking to old file_ids. If no past loads for the data source,
 * a user task will be generated. Either way, a verification task is created to show the current state of the plotting
 * fields in the output modal
 */
@SystemTask(taskId = 20, taskName = "Set Plotting Fields")
fun setPlottingFields(connection: Connection, prTask: PipelineRunTask) {
    val lastRun = PipelineRuns.lastRun(connection, prTask.runId)
    if (lastRun == null) {
        PipelineRunTasks.addTask(connection, prTask.pipelineRunTaskId, MANUALLY_SET_PLOTTING_FIELDS)
    } else {
        connection.executeNoReturn(
            sql = """
                INSERT INTO ${PlottingFields.tableName}(
                        run_id,file_id,name,address_line1,address_line2,city,alternate_cities,mailing_code,latitude,
                        longitude,prov,clean_address,clean_city
                )
                SELECT t2.run_id,t1.file_id,t1.name,t1.address_line1,t1.address_line2,t1.city,t1.alternate_cities,
                       t1.mailing_code,t1.latitude,t1.longitude,t1.prov,t1.clean_address,t1.clean_city
                FROM   ${PlottingFields.tableName} t1
                JOIN   ${SourceTables.tableName} t2
                ON     t1.file_id = t2.file_id
                WHERE  t1.run_id = ?
                AND    t2.run_id = ?;
            """.trimIndent(),
            lastRun,
            prTask.runId,
        )
    }
}

/**
 * System task to bring forward past plotting methods. If no past loads for the data source, a user task will be
 * generated. Either way, a verification task is created to show the current state of the plotting methods in the output
 * modal
 */
@SystemTask(taskId = 23, taskName = "Set Plotting Methods")
fun setPlottingMethods(connection: Connection, prTask: PipelineRunTask) {
    val lastRun = PipelineRuns.lastRun(connection, prTask.runId)
    if (lastRun == null) {
        PipelineRunTasks.addTask(connection, prTask.pipelineRunTaskId, MANUALLY_SET_PLOTTING_METHODS)
    } else {
        connection.executeNoReturn(
            sql = """
                INSERT INTO ${PlottingMethods.tableName}(run_id,plotting_order,method_type,file_id)
                SELECT t2.run_id, t1.plotting_order, t1.method_type, t2.file_id
                FROM   ${PlottingMethods.tableName} t1
                JOIN   ${SourceTables.tableName} t2
                ON     t1.file_id = t2.file_id
                WHERE  t1.run_id = ?
                AND    t2.run_id = ?;
            """.trimIndent(),
            lastRun,
            prTask.runId,
        )
    }
}

/**
 * System task to generate the steps required to set up the loading logic for the current run
 */
@SystemTask(taskId = 26, taskName = "Set Loading Logic")
fun setLoadingLogic(connection: Connection, prTask: PipelineRunTask) {
    PipelineRunTasks.addTask(connection, prTask.pipelineRunTaskId, getTaskIdFromFunction(::setPlottingFields))
    PipelineRunTasks.addTask(connection, prTask.pipelineRunTaskId, getTaskIdFromFunction(::setPlottingMethods))
}
