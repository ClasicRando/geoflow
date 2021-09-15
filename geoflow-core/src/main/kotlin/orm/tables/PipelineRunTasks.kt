package orm.tables

import database.DatabaseConnection
import formatInstantDefault
import kotlinx.serialization.Serializable
import org.ktorm.dsl.*
import org.ktorm.schema.*
import org.ktorm.support.postgresql.LockingMode
import org.ktorm.support.postgresql.insertOrUpdateReturning
import org.ktorm.support.postgresql.locking
import orm.entities.PipelineRunTask

object PipelineRunTasks: Table<PipelineRunTask>("pipeline_run_tasks") {

    val pipelineRunTaskId = long("pr_task_id").primaryKey().bindTo { it.pipelineRunTaskId }
    val runId = long("run_id").bindTo { it.runId }
    val taskRunning = boolean("task_running").bindTo { it.taskRunning }
    val taskComplete = boolean("task_complete").bindTo { it.taskComplete }
    val taskStart = timestamp("task_start").bindTo { it.taskStart }
    val taskCompleted = timestamp("task_completed").bindTo { it.taskCompleted }
    val taskId = long("task_id").references(Tasks) { it.task }
    val taskMessage = text("task_message").bindTo { it.taskMessage }
    val runTaskOrder = int("run_task_order").bindTo { it.runTaskOrder }

    val tableDisplayFields = mapOf(
        "taskName" to mapOf("name" to "Task Name"),
        "taskRunning" to mapOf("name" to "Running"),
        "taskComplete" to mapOf("name" to "Complete"),
        "taskStart" to mapOf("name" to "Start"),
        "taskCompleted" to mapOf("name" to "Completed"),
    )

    val createStatement = """
        CREATE TABLE IF NOT EXISTS public.pipeline_run_tasks
        (
            pr_task_id bigint NOT NULL DEFAULT nextval('pipeline_run_tasks_pr_task_id_seq'::regclass),
            run_id bigint NOT NULL,
            task_running boolean NOT NULL,
            task_complete boolean NOT NULL,
            task_start timestamp without time zone,
            task_completed timestamp without time zone,
            task_id bigint,
            task_message text COLLATE pg_catalog."default",
            CONSTRAINT pipeline_run_tasks_pkey PRIMARY KEY (pr_task_id)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    val createSequence = """
        CREATE SEQUENCE public.pipeline_run_tasks_pr_task_id_seq
            INCREMENT 1
            START 1
            MINVALUE 1
            MAXVALUE 9223372036854775807
            CACHE 1;
    """.trimIndent()

    fun reserveRecord(pipelineRunTaskId: Long): PipelineRunTask {
        return DatabaseConnection
            .database
            .from(this)
            .select()
            .where(this.pipelineRunTaskId eq pipelineRunTaskId)
            .locking(LockingMode.FOR_SHARE)
            .map(this::createEntity)
            .first()
    }

    fun getRecord(pipelineRunTaskId: Long): PipelineRunTask {
        return DatabaseConnection
            .database
            .from(this)
            .select()
            .where(this.pipelineRunTaskId eq pipelineRunTaskId)
            .map(this::createEntity)
            .first()
    }

    fun addTask(runId: Long, taskId: Long): Long? {
        return DatabaseConnection
            .database
            .insertOrUpdateReturning(this, pipelineRunTaskId) {
                set(PipelineRunTasks.runId, runId)
                set(taskRunning, false)
                set(taskComplete, false)
                set(taskStart, null)
                set(taskCompleted, null)
                set(PipelineRunTasks.taskId, taskId)
                onConflict(PipelineRunTasks.runId, PipelineRunTasks.taskId) {
                    set(taskRunning, false)
                    set(taskComplete, false)
                    set(taskStart, null)
                    set(taskCompleted, null)
                }
            }
    }

    @Serializable
    data class Record(
        val pipelineRunTaskId: Long,
        val runId: Long,
        val taskRunning: Boolean,
        val taskComplete: Boolean,
        val taskStart: String,
        val taskCompleted: String,
        val taskName: String,
    )

    fun getTasks(runId: Long): List<Record> {
        return DatabaseConnection
            .database
            .from(this)
            .joinReferencesAndSelect()
            .where(this.runId eq runId)
            .map(this::createEntity)
            .map {
                Record(
                    it.pipelineRunTaskId,
                    it.runId,
                    it.taskRunning,
                    it.taskComplete,
                    formatInstantDefault(it.taskStart),
                    formatInstantDefault(it.taskCompleted),
                    it.task.name
                )
            }
    }

    @Throws(IllegalArgumentException::class)
    fun getNextTask(runId: Long): PipelineRunTask {
        val running = DatabaseConnection
            .database
            .from(this)
            .select(taskId)
            .where((this.runId eq runId) and (taskRunning eq true))
            .limit(1)
            .map { row -> row[taskId] }
            .firstOrNull()
        if (running != null) {
            throw IllegalArgumentException("Task currently running. $running")
        }
        return DatabaseConnection
            .database
            .from(this)
            .joinReferencesAndSelect()
            .where((this.runId eq runId) and (taskComplete eq false))
            .orderBy(runTaskOrder.asc())
            .limit(1)
            .map(this::createEntity)
            .first()
    }
}