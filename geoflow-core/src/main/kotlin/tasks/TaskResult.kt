package tasks

sealed interface TaskResult {
    data class Success(val message: String? = null): TaskResult
    data class Error(val throwable: Throwable): TaskResult {
        val message = "ERROR in Task: ${throwable.message ?: throwable.toString()}"
    }
}