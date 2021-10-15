import database.DatabaseConnection
import it.justwrote.kjob.*
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

private val logger = KotlinLogging.logger {}

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

fun main() {
    val kjob = startKjob(false)
    kjob.register(SystemJob) {
        executionType = JobExecutionType.NON_BLOCKING
        maxRetries = 0
        execute {
            try {
                val runId = props[it.runId]
                val task = ClassLoader
                    .getSystemClassLoader()
                    .loadClass("tasks.${props[it.taskClassName]}")
                    .getConstructor(Long::class.java)
                    .newInstance(props[it.pipelineRunTaskId]) as SystemTask
                val taskResult = task.runTask()
                if (taskResult && props[it.runNext]) {
                    PipelineRunTasks.getNextTask(props[it.runId])?.let { nextTask ->
                        if (nextTask.taskRunType == TaskRunType.System) {
                            kjob.schedule(SystemJob) {
                                props[it.runId] = runId
                                props[it.pipelineRunTaskId] = nextTask.taskId
                                props[it.taskClassName] = nextTask.taskClassName
                                props[it.runNext] = true
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                withContext(NonCancellable) {
                    val pipelineRunTaskId = props[it.pipelineRunTaskId]
                    DatabaseConnection.database.update(PipelineRunTasks) {
                        set(PipelineRunTasks.taskMessage, "ERROR in Job: ${t.message}")
                        set(PipelineRunTasks.taskStatus, TaskStatus.Failed)
                        set(PipelineRunTasks.taskCompleted, null)
                        where { PipelineRunTasks.pipelineRunTaskId eq pipelineRunTaskId }
                    }
                }
            }
        }
    }
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down worker")
        kjob.shutdown()
    })
}