package jobs

import it.justwrote.kjob.Job
import tasks.SystemTask

/**
 * Generic job for kjob to run a [SystemTask]. Requires a [runId], [pipelineRunTaskId] and [taskClassName] to find the
 * [SystemTask] function and class then function, as well as a [runNext] flag to denote if the user asked to keep
 * running tasks after the first one is completed. Subsequent tasks are only run when next task is a [SystemTask] and
 * the current task finished successfully
 *
 * No execution code is included here, but it can be found in Main.kt of the geoflow-worker module.
 */
object SystemJob : Job("system-job") {
    val runId = long("runId")
    val pipelineRunTaskId = long("pipelineRunTaskId")
    val taskClassName = string("taskClassName")
    val runNext = bool("runNext")
}
