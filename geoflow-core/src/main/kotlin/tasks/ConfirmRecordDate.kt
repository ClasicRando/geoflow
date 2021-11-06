package tasks

/**
 * User task to validate that the user has confirmed the record date of the pipeline run is correct
 */
class ConfirmRecordDate(pipelineRunTaskId: Long): UserTask(pipelineRunTaskId) {

    override val taskId: Long = 6
}