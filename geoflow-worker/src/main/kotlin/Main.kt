import database.DatabaseConnection
import it.justwrote.kjob.Mongo
import it.justwrote.kjob.job.JobExecutionType
import it.justwrote.kjob.kjob
import jobs.SystemJob
import mu.KotlinLogging
import orm.enums.TaskRunType
import orm.enums.TaskStatus
import orm.tables.PipelineRunTasks
import tasks.SystemTask

fun main() {
    val logger = KotlinLogging.logger {}
    val kjob = kjob(Mongo) {
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
    kjob.register(SystemJob) {
        executionType = JobExecutionType.NON_BLOCKING
        maxRetries = 0
        execute {
            runCatching {
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
            }.getOrElse { t ->
                val pipelineRunTaskId = props[it.pipelineRunTaskId]
                DatabaseConnection.database.useTransaction {
                    val taskRecord = PipelineRunTasks.reserveRecord(pipelineRunTaskId)
                    taskRecord.taskMessage = "ERROR: ${t.message}"
                    taskRecord.taskStatus = TaskStatus.Failed
                    taskRecord.taskCompleted = null
                    taskRecord.flushChanges()
                }
            }
        }
    }
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down worker")
        kjob.shutdown()
    })
}