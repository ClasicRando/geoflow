package me.geoflow.core.tasks

/**
 * Field annotation to denote that a property is the constant value of a task
 *
 * @param taskName name of the task as seen in the database. Used to check and make sure the database and code are
 * in sync
 */
@Target(AnnotationTarget.FIELD)
annotation class UserTask(val taskName: String)
