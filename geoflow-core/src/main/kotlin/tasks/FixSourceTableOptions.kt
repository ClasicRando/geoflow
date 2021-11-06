package tasks

/**
 * User task to notify the user the source table options are invalid
 */
class FixSourceTableOptions(pipelineRunTaskId: Long): UserTask(pipelineRunTaskId) {

    override val taskId: Long = 9
}