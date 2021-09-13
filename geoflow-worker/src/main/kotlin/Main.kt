import it.justwrote.kjob.Mongo
import it.justwrote.kjob.job.JobExecutionType
import it.justwrote.kjob.kjob
import jobs.SystemJob
import mu.KotlinLogging
import tasks.SystemTask

fun main() {
    val logger = KotlinLogging.logger {}
    val kjob = kjob(Mongo) {
        nonBlockingMaxJobs = 10
        blockingMaxJobs = 1
        maxRetries = 3
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
        maxRetries = 3
        execute {
            val className = props[it.taskClassName]
            val task = ClassLoader
                .getSystemClassLoader()
                .loadClass("tasks.$className")
                .getConstructor(Long::class.java)
                .newInstance(props[it.pipelineRunTaskId]) as SystemTask
            task.runTask()
        }
    }
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down worker")
        kjob.shutdown()
    })
}