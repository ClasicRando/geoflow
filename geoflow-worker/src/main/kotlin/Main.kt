import it.justwrote.kjob.Mongo
import it.justwrote.kjob.job.JobExecutionType
import it.justwrote.kjob.kjob
import jobs.SystemJob
import mu.KotlinLogging
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
            val className = PipelineRunTasks.getTaskClassName(props[it.pipelineRunTaskId])
            val task = ClassLoader
                .getSystemClassLoader()
                .loadClass("tasks.$className")
                .getConstructor(Long::class.java)
                .newInstance(props[it.pipelineRunTaskId]) as SystemTask
            val taskResult = task.runTask()
            if (taskResult && task.nextTaskId != null) {
                kjob.schedule(SystemJob) {
                    props[it.pipelineRunTaskId] = task.nextTaskId
                }
            }
        }
    }
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down worker")
        kjob.shutdown()
    })
}