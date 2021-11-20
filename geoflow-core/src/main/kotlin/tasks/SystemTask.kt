package tasks

/**
 * Function annotation to mark the function as a system task that is runnable by the worker. On worker setup, reflection
 * is used to find all annotated system tasks to register them for later usage
 *
 * @param taskId unique ID for the task to be found for running later
 */
@Target(AnnotationTarget.FUNCTION)
annotation class SystemTask(val taskId: Long)
