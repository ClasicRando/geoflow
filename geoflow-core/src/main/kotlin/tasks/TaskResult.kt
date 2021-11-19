package tasks

/**
 * Defines class that can be returned as a result of a pipeline task. Sealed to contain possible result types
 */
sealed interface TaskResult {
    /** Task run was successful. Provides an optional message if the task wants to report warning or detail */
    data class Success(val message: String? = null) : TaskResult
    /** Task run has an error. Provides the throwable instance to get message and stack trace for tracking */
    data class Error(val throwable: Throwable) : TaskResult {
        val message = "ERROR in Task: ${throwable.message ?: throwable.toString()}"
    }
}
