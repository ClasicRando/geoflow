package jobs

import it.justwrote.kjob.Job

object SystemJob: Job("system-job") {
    val runId = long("runId")
    val pipelineRunTaskId = long("pipelineRunTaskId")
    val taskClassName = string("taskClassName")
    val runNext = bool("runNext")
}