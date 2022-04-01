package me.geoflow.worker

import me.geoflow.core.database.Database
import me.geoflow.core.database.enums.TaskRunType
import me.geoflow.core.database.tables.PipelineRunTasks
import it.justwrote.kjob.InMem
import it.justwrote.kjob.KJob
import it.justwrote.kjob.Mongo
import it.justwrote.kjob.dsl.JobContextWithProps
import it.justwrote.kjob.job.JobExecutionType
import it.justwrote.kjob.kjob
import me.geoflow.core.jobs.SystemJob
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import me.geoflow.core.tasks.TaskResult
import me.geoflow.core.tasks.runTask

private val logger = KotlinLogging.logger {}

/** Utility to start Kjob using a MongoDB or stored in memory (for testing). */
@Suppress("MagicNumber")
fun startKjob(isMongo: Boolean): KJob {
    return if (isMongo) {
        kjob(Mongo) {
            nonBlockingMaxJobs = 10
            blockingMaxJobs = 1
            maxRetries = 0
            defaultJobExecutor = JobExecutionType.NON_BLOCKING

            exceptionHandler = { t -> logger.error("Unhandled exception", t)}
            keepAliveExecutionPeriodInSeconds = 60
            jobExecutionPeriodInSeconds = 1
            cleanupPeriodInSeconds = 300
            cleanupSize = 50

            connectionString = "mongodb://127.0.0.1:27017"
            databaseName = "kjob"
            jobCollection = "kjob-jobs"
            lockCollection = "kjob-locks"
            expireLockInMinutes = 5L
        }.start()
    } else {
        kjob(InMem) {
            nonBlockingMaxJobs = 10
            blockingMaxJobs = 1
            maxRetries = 0
            defaultJobExecutor = JobExecutionType.NON_BLOCKING

            exceptionHandler = { t -> logger.error("Unhandled exception", t)}
            keepAliveExecutionPeriodInSeconds = 60
            jobExecutionPeriodInSeconds = 1
            cleanupPeriodInSeconds = 300
            cleanupSize = 50

            expireLockInMinutes = 5L
        }.start()
    }
}

/**
 * Execution function for the Base System job. Runs the task and handled the result to update the task record.
 *
 * Extracts the properties of the scheduled job to create a task class instance using the class name and pipeline task
 * ID. The task is then run to get the [TaskResult]. If it is a [success][TaskResult.Success], the database record is
 * updated and if the 'runNext' property is true then the next task for the pipeline run is scheduled to run. If the
 * task result is an [error][TaskResult.Error], the database record is updated with the message and stacktrace.
 */
@Suppress("LongMethod")
suspend fun JobContextWithProps<SystemJob>.executeSystemJob(kJob: KJob) {
    runCatching {
        val pipelineRunTaskId = props[SystemJob.pipelineRunTaskId]
        val runId = props[SystemJob.runId]
        val result = runTask(pipelineRunTaskId)
        Database.runWithConnectionAsync { connection ->
            PipelineRunTasks.finalizeTaskRun(connection, pipelineRunTaskId, result)
            if (result is TaskResult.Success && props[SystemJob.runNext]) {
                PipelineRunTasks.getNextTask(connection, props[SystemJob.runId])?.let { nextTask ->
                    if (nextTask.taskRunType is TaskRunType.System) {
                        kJob.schedule(SystemJob) {
                            props[it.runId] = runId
                            props[it.pipelineRunTaskId] = nextTask.taskId
                            props[it.runNext] = true
                        }
                    }
                }
            }
        }
    }.getOrElse { t ->
        withContext(NonCancellable) {
            val pipelineRunTaskId = props[SystemJob.pipelineRunTaskId]
            Database.runWithConnection {
                PipelineRunTasks.finalizeTaskRun(it, pipelineRunTaskId, t)
            }
        }
    }
}

/**
 * Main entry function for the worker. Starts Kjob and registers the Jobs that this Kjob instance is able to run.
 *
 * Currently, the only job registered is [SystemJob] which runs systems tasks scheduled by the server.
 *
 * To complete then worker setup, a shutdown hook is added to peacefully shut down the worker.
 */
fun main() {
    val kjob = startKjob(true)
    kjob.register(SystemJob) {
        executionType = JobExecutionType.NON_BLOCKING
        maxRetries = 0
        execute {
            executeSystemJob(kjob)
        }
    }
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down worker")
        kjob.shutdown()
    })
}
