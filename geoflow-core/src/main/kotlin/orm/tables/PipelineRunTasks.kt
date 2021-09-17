package orm.tables

import database.DatabaseConnection
import database.functions.GetTasksOrdered
import formatInstantDateTime
import kotlinx.serialization.Serializable
import org.ktorm.dsl.*
import org.ktorm.schema.*
import org.ktorm.support.postgresql.LockingMode
import org.ktorm.support.postgresql.insertOrUpdateReturning
import org.ktorm.support.postgresql.locking
import orm.entities.PipelineRunTask
import orm.enums.TaskStatus
import java.sql.Timestamp

object PipelineRunTasks: Table<PipelineRunTask>("pipeline_run_tasks") {

    val pipelineRunTaskId = long("pr_task_id").primaryKey().bindTo { it.pipelineRunTaskId }
    val runId = long("run_id").bindTo { it.runId }
    val taskStart = timestamp("task_start").bindTo { it.taskStart }
    val taskCompleted = timestamp("task_completed").bindTo { it.taskCompleted }
    val taskId = long("task_id").references(Tasks) { it.task }
    val taskMessage = text("task_message").bindTo { it.taskMessage }
    val parentTaskId = long("parent_task_id").bindTo { it.parentTaskId }
    val parentTaskOrder = int("parent_task_order").bindTo { it.parentTaskOrder }
    val taskStatus = enum<TaskStatus>("task_status").bindTo { it.taskStatus }

    val tableDisplayFields = mapOf(
        "taskName" to mapOf("name" to "Task Name"),
        "taskStatus" to mapOf("name" to "Status"),
        "taskStart" to mapOf("name" to "Start"),
        "taskCompleted" to mapOf("name" to "Completed"),
    )

    val createStatement = """
        CREATE TABLE IF NOT EXISTS public.pipeline_run_tasks
        (
            pr_task_id bigint NOT NULL DEFAULT nextval('pipeline_run_tasks_pr_task_id_seq'::regclass),
            run_id bigint NOT NULL,
            task_start timestamp without time zone,
            task_completed timestamp without time zone,
            task_id bigint NOT NULL,
            task_message text COLLATE pg_catalog."default",
            task_status task_status NOT NULL,
            parent_task_id bigint NOT NULL DEFAULT 0,
            parent_task_order bigint NOT NULL,
            CONSTRAINT pipeline_run_tasks_pkey PRIMARY KEY (pr_task_id),
            CONSTRAINT task_per_run UNIQUE (run_id, task_id),
            CONSTRAINT run_id FOREIGN KEY (pr_task_id)
                REFERENCES public.pipeline_runs (run_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE
                NOT VALID
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

    val createEnums = """
        CREATE TYPE public.task_status AS ENUM
            ('Waiting', 'Scheduled', 'Running', 'Complete');
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
                set(taskStatus, TaskStatus.Waiting)
                set(taskStart, null)
                set(taskCompleted, null)
                set(PipelineRunTasks.taskId, taskId)
                onConflict(PipelineRunTasks.runId, PipelineRunTasks.taskId) {
                    set(taskStatus, TaskStatus.Waiting)
                    set(taskStart, null)
                    set(taskCompleted, null)
                }
            }
    }

    @Serializable
    data class Record(
        val pipelineRunTaskId: Long,
        val runId: Long,
        val taskStatus: String,
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
                    it.taskStatus.name,
                    formatInstantDateTime(it.taskStart),
                    formatInstantDateTime(it.taskCompleted),
                    it.task.name,
                )
            }
    }

    fun getOrderedTasks(runId: Long): List<Record> {
        return GetTasksOrdered.call(runId).map { row ->
            Record(
                row["pr_task_id"] as Long,
                row["run_id"] as Long,
                TaskStatus.valueOf(row["task_status"] as String).name,
                formatInstantDateTime((row["task_start"] as Timestamp?)?.toInstant()),
                formatInstantDateTime((row["task_completed"] as Timestamp?)?.toInstant()),
                row["task_name"] as String
            )
        }
    }

    @Throws(IllegalArgumentException::class)
    fun getNextTask(runId: Long): PipelineRunTask {
        val running = DatabaseConnection
            .database
            .from(this)
            .select(taskId)
            .whereWithConditions {
                it += this.runId eq runId
                it += taskStatus.inList(TaskStatus.Scheduled, TaskStatus.Running)
            }
            .limit(1)
            .map { row -> row[taskId] }
            .firstOrNull()
        if (running != null) {
            throw IllegalArgumentException("Task currently scheduled/running (id = $running)")
        }
        return DatabaseConnection
            .database
            .from(this)
            .joinReferencesAndSelect()
            .where((this.runId eq runId) and (taskStatus eq TaskStatus.Waiting))
            .limit(1)
            .map(this::createEntity)
            .first()
    }
}