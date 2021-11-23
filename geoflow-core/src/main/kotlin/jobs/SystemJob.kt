package jobs

import it.justwrote.kjob.Job
import it.justwrote.kjob.Prop
import tasks.SystemTask

/**
 * Generic job for kjob to run a [SystemTask]. Requires a [runId] and [pipelineRunTaskId] to find the [SystemTask]
 * function and class then function, as well as a [runNext] flag to denote if the user asked to keep running tasks
 * after the first one is completed. Subsequent tasks are only run when next task is a [SystemTask] and the current
 * task finished successfully
 *
 * No execution code is included here, but it can be found in Main.kt of the geoflow-worker module.
 */
object SystemJob : Job("system-job") {
    /** run_id of the pipeline run task to be executed */
    val runId: Prop<SystemJob, Long> = long("runId")
    /** pr_task_id ID of the pipeline run task to be executed */
    val pipelineRunTaskId: Prop<SystemJob, Long> = long("pipelineRunTaskId")
    /**
     * flag denoting if the next task should be run if the current task has no errors and the next task is a System task
     */
    val runNext: Prop<SystemJob, Boolean> = bool("runNext")
}
