package jobs

import it.justwrote.kjob.Job

object SystemJob: Job("system-job") {
    val pipelineRunTaskId = long("pipelineRunTaskId")
}