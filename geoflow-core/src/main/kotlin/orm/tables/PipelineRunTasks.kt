package orm.tables

import database.DatabaseConnection
import database.functions.GetTasksOrdered
import formatInstantDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.ktorm.dsl.*
import org.ktorm.schema.*
import org.ktorm.support.postgresql.LockingMode
import org.ktorm.support.postgresql.insertReturning
import org.ktorm.support.postgresql.locking
import orm.entities.PipelineRunTask
import orm.enums.TaskRunType
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
        "task_name" to mapOf("name" to "Task Name"),
        "task_run_type" to mapOf("name" to "Run Type"),
        "task_status" to mapOf("name" to "Status"),
        "task_start" to mapOf("name" to "Start"),
        "task_completed" to mapOf("name" to "Completed"),
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
            task_status task_status NOT NULL DEFAULT 'Waiting'::task_status,
            parent_task_id bigint NOT NULL DEFAULT 0,
            parent_task_order integer NOT NULL,
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
            .joinReferencesAndSelect()
            .where(this.pipelineRunTaskId eq pipelineRunTaskId)
            .map(this::createEntity)
            .first()
    }

    @Throws(IllegalArgumentException::class)
    fun getRecordForRun(username: String, runId: Long, pipelineRunTaskId: Long): PipelineRunTask {
        if (!PipelineRuns.checkUserRun(runId, username)) {
            throw IllegalArgumentException("User provided cannot run tasks for this pipeline run")
        }
        val record = getRecord(pipelineRunTaskId)
        return when {
            record.runId != runId ->
                throw IllegalArgumentException("Specified run task is not part of the run referenced")
            record.taskStatus != TaskStatus.Waiting ->
                throw IllegalArgumentException("Specified run task must be waiting to be run")
            else -> {
                val nextTask = getNextTask(runId) ?: throw IllegalArgumentException("Cannot find next task")
                if (nextTask.pipelineRunTaskId != record.pipelineRunTaskId)
                    throw IllegalArgumentException("Selected task to run is not next")
                record
            }
        }
    }

    fun addTask(pipelineRunTask: PipelineRunTask, taskId: Long): Long? {
        val nextOrder = DatabaseConnection
            .database
            .from(this)
            .select(max(parentTaskOrder))
            .where(parentTaskId eq pipelineRunTask.task.taskId)
            .map { row -> row.getInt(1) }
            .firstOrNull() ?: 1
        return DatabaseConnection
            .database
            .insertReturning(this, pipelineRunTaskId) {
                set(runId, pipelineRunTask.runId)
                set(taskStatus, TaskStatus.Waiting)
                set(taskStart, null)
                set(taskCompleted, null)
                set(PipelineRunTasks.taskId, taskId)
                set(parentTaskId, pipelineRunTask.pipelineRunTaskId)
                set(parentTaskOrder, nextOrder)
            }
    }

    @Serializable
    data class Record(
        @SerialName("task_order")
        val taskOrder: Long,
        @SerialName("pipeline_run_task_id")
        val pipelineRunTaskId: Long,
        @SerialName("run_id")
        val runId: Long,
        @SerialName("task_start")
        val taskStart: String,
        @SerialName("task_completed")
        val taskCompleted: String,
        @SerialName("task_id")
        val taskId: Long,
        @SerialName("task_message")
        val taskMessage: String,
        @SerialName("task_status")
        val taskStatus: String,
        @SerialName("parent_task_id")
        val parentTaskId: Long,
        @SerialName("parent_task_order")
        val parentTaskOrder: Int,
        @SerialName("task_name")
        val taskName: String,
        @SerialName("task_description")
        val taskDescription: String,
        @SerialName("task_class_name")
        val taskClassName: String,
        @SerialName("task_run_type")
        val taskRunType: String,
    )

    @Throws(IllegalArgumentException::class)
    fun getOrderedTasks(runId: Long): List<Record> {
        return GetTasksOrdered.call(runId).map { row ->
            Record(
                row["task_order"] as Long,
                row["pr_task_id"] as Long,
                row["run_id"] as Long,
                formatInstantDateTime((row["task_start"] as Timestamp?)?.toInstant()),
                formatInstantDateTime((row["task_completed"] as Timestamp?)?.toInstant()),
                row["task_id"] as Long,
                (row["task_message"] as String?) ?: "",
                row["task_status"] as String,
                row["parent_task_id"] as Long,
                row["parent_task_order"] as Int,
                row["task_name"] as String,
                row["task_description"] as String,
                row["task_class_name"] as String,
                row["task_run_type"] as String,
            )
        }
    }

    data class NextTask(
        val pipelineRunTaskId: Long,
        val taskId: Long,
        val taskRunType: TaskRunType,
        val taskClassName: String,
    )

    @Throws(IllegalArgumentException::class)
    fun getNextTask(runId: Long): NextTask? {
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
        return GetTasksOrdered.nextToRun(runId)?.let { task ->
            NextTask(
                task["pr_task_id"] as Long,
                task["task_id"] as Long,
                TaskRunType.valueOf(task["task_run_type"] as String),
                task["task_class_name"] as String,
            )
        }
    }

    fun setStatus(pipelineRunTaskId: Long, status: TaskStatus) {
        DatabaseConnection
            .database
            .update(this) {
                set(taskStatus, status)
                where { this@PipelineRunTasks.pipelineRunTaskId eq pipelineRunTaskId }
            }
    }
}