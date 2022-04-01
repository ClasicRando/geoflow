package me.geoflow.core.tasks

/**
 * Defines class that can be returned as a result of a pipeline task. Sealed to contain possible result types
 */
sealed interface TaskResult {
    /**
     * Task run was successful. Provides an optional message if the task wants to report warning or detail
     *
     * @param message optional message for successful task completion
     */
    data class Success(val message: String? = null) : TaskResult
    /**
     * Task run has an error. Provides the throwable instance to get message and stack trace for tracking
     *
     * @param throwable exception thrown when the task encounters an error
     */
    data class Error(val throwable: Throwable) : TaskResult {
        /** Basic message from the throwable */
        val message: String = "ERROR in Task: ${throwable.message ?: throwable.toString()}"
    }
    /**
     * Task run had a rule broken while trying to validate some rules. Object since information is stored in the
     * [PipelineRunTaskRules][me.geoflow.core.database.tables.PipelineRunTaskRules]
     */
    object RuleBroken : TaskResult
}
