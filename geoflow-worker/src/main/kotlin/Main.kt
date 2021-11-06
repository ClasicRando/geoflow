import database.DatabaseConnection
import it.justwrote.kjob.*
import it.justwrote.kjob.dsl.JobContextWithProps
import it.justwrote.kjob.dsl.KJobFunctions
import it.justwrote.kjob.job.JobExecutionType
import jobs.SystemJob
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.ktorm.dsl.eq
import org.ktorm.dsl.update
import orm.enums.TaskRunType
import orm.enums.TaskStatus
import orm.tables.PipelineRunTasks
import tasks.SystemTask
import tasks.TaskResult
import java.time.Instant

private val logger = KotlinLogging.logger {}

/** Utility to start Kjob using a MongoDB or stored in memory (for testing). */
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
suspend fun JobContextWithProps<SystemJob>.executeSystemJob(kJob: KJob) {
    try {
        val pipelineRunTaskId = props[SystemJob.pipelineRunTaskId]
        val runId = props[SystemJob.runId]
        val task = ClassLoader
            .getSystemClassLoader()
            .loadClass("tasks.${props[SystemJob.taskClassName]}")
            .getConstructor(Long::class.java)
            .newInstance(props[SystemJob.pipelineRunTaskId]) as SystemTask
        when(val result = task.runTask()) {
            is TaskResult.Success -> {
                DatabaseConnection.database.update(PipelineRunTasks) {
                    set(it.taskMessage, result.message)
                    set(it.taskStatus, TaskStatus.Complete)
                    set(it.taskCompleted, Instant.now())
                    set(it.taskStackTrace, null)
                    where { it.pipelineRunTaskId eq pipelineRunTaskId }
                }
                if (props[SystemJob.runNext]) {
                    PipelineRunTasks.getNextTask(props[SystemJob.runId])?.let { nextTask ->
                        if (nextTask.taskRunType == TaskRunType.System) {
                            kJob.schedule(SystemJob) {
                                props[it.runId] = runId
                                props[it.pipelineRunTaskId] = nextTask.taskId
                                props[it.taskClassName] = nextTask.taskClassName
                                props[it.runNext] = true
                            }
                        }
                    }
                }
            }
            is TaskResult.Error -> {
                DatabaseConnection.database.update(PipelineRunTasks) {
                    set(it.taskMessage, result.message)
                    set(it.taskStatus, TaskStatus.Failed)
                    set(it.taskCompleted, null)
                    set(it.taskStackTrace, result.throwable.stackTraceToString())
                    where { it.pipelineRunTaskId eq pipelineRunTaskId }
                }
            }
        }
    } catch (t: Throwable) {
        withContext(NonCancellable) {
            val pipelineRunTaskId = props[SystemJob.pipelineRunTaskId]
            DatabaseConnection.database.update(PipelineRunTasks) {
                set(it.taskMessage, "ERROR in Job: ${t.message}")
                set(it.taskStatus, TaskStatus.Failed)
                set(it.taskCompleted, null)
                set(it.taskStackTrace, t.stackTraceToString())
                where { it.pipelineRunTaskId eq pipelineRunTaskId }
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