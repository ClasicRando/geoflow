package me.geoflow.core.database.tables

import kotlinx.html.DIV
import kotlinx.html.div
import kotlinx.html.stream.appendHTML
import me.geoflow.core.database.enums.TaskRunType
import me.geoflow.core.database.errors.NoRecordAffected
import me.geoflow.core.database.errors.NoRecordFound
import me.geoflow.core.database.enums.TaskStatus
import me.geoflow.core.database.enums.TaskStatus.Companion.isTaskStatus
import me.geoflow.core.database.errors.TaskFailedError
import me.geoflow.core.database.functions.GetTasksOrdered
import me.geoflow.core.database.extensions.queryFirstOrNull
import me.geoflow.core.database.extensions.runReturningFirstOrNull
import me.geoflow.core.database.extensions.runUpdate
import me.geoflow.core.database.functions.UserHasRun
import me.geoflow.core.database.errors.TaskRunningException
import me.geoflow.core.database.extensions.submitQuery
import me.geoflow.core.database.procedures.ResetTask
import me.geoflow.core.database.tables.records.NextTask
import me.geoflow.core.database.tables.records.PipelineRunTask
import me.geoflow.core.database.tables.records.ResponsePrTask
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant

/**
 * Table used to store tasks that are associated with a specified run
 *
 * Records contain metadata about the task, and it's run status/outcome.
 */
@Suppress("TooManyFunctions")
object PipelineRunTasks: DbTable("pipeline_run_tasks"), ApiExposed, Triggers {

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "task_status" to mapOf("title" to "Status", "formatter" to "statusFormatter"),
        "task_name" to mapOf("title" to "Name"),
        "task_run_type" to mapOf("title" to "Run Type"),
        "task_description" to mapOf("title" to "Description"),
        "time" to mapOf("title" to "Time (mins)", "formatter" to "taskTimeFormatter"),
        "actions" to mapOf("formatter" to "taskActionFormatter"),
    )

    /** */
    val subTableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "pipeline_run_task_id" to mapOf("title" to "ID"),
        "task_id" to mapOf("title" to "Generic Task ID"),
        "task_start" to mapOf("title" to "Start"),
        "task_completed" to mapOf("title" to "Completed"),
        "task_message" to mapOf("title" to "Task Message"),
        "task_stack_trace" to mapOf("title" to "Stack Trace"),
    )

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.pipeline_run_tasks
        (
            pr_task_id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            run_id bigint NOT NULL REFERENCES public.pipeline_runs (run_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
            task_start timestamp with time zone,
            task_completed timestamp with time zone,
            task_id bigint NOT NULL REFERENCES public.tasks (task_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            task_message text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(task_message)),
            task_status task_status NOT NULL DEFAULT 'Waiting'::task_status,
            parent_task_id bigint NOT NULL DEFAULT 0,
            parent_task_order integer NOT NULL,
            workflow_operation text COLLATE pg_catalog."default" NOT NULL
                REFERENCES public.workflow_operations (code) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE RESTRICT,
            task_stack_trace text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(task_stack_trace)),
            modal_html text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(modal_html))
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val triggers: List<Trigger> = listOf(
        Trigger(
            trigger = """
                CREATE TRIGGER delete_event
                    AFTER DELETE
                    ON public.pipeline_run_tasks
                    REFERENCING OLD TABLE AS old_table
                    FOR EACH STATEMENT
                    EXECUTE FUNCTION public.pipeline_run_tasks_event();
            """.trimIndent(),
            triggerFunction = """
                CREATE OR REPLACE FUNCTION public.pipeline_run_tasks_event()
                    RETURNS trigger
                    LANGUAGE 'plpgsql'
                    COST 100
                    VOLATILE NOT LEAKPROOF
                AS ${'$'}BODY${'$'}
                DECLARE
                    runId bigint;
                BEGIN
                    IF TG_OP = 'INSERT' THEN
                      SELECT DISTINCT run_id
                      INTO   runId
                      FROM   new_table;
                    ELSIF TG_OP = 'DELETE' THEN
                      SELECT DISTINCT run_id
                      INTO   runId
                      FROM   old_table;
                    ELSE
                      SELECT DISTINCT run_id
                      INTO   runId
                      FROM   new_table;
                    END IF;
                    PERFORM pg_notify(
                      'pipelineruntasks',
                      runId::text
                    );
                    RETURN NULL;
                END;
                ${'$'}BODY${'$'};
            """.trimIndent()
        ),
        Trigger(
            trigger = """
                CREATE TRIGGER insert_event
                    AFTER INSERT
                    ON public.pipeline_run_tasks
                    REFERENCING NEW TABLE AS new_table
                    EXECUTE FUNCTION public.pipeline_run_tasks_event();
            """.trimIndent(),
            triggerFunction = """
                CREATE OR REPLACE FUNCTION public.pipeline_run_tasks_event()
                    RETURNS trigger
                    LANGUAGE 'plpgsql'
                    COST 100
                    VOLATILE NOT LEAKPROOF
                AS ${'$'}BODY${'$'}
                DECLARE
                    runId bigint;
                BEGIN
                    IF TG_OP = 'INSERT' THEN
                      SELECT DISTINCT run_id
                      INTO   runId
                      FROM   new_table;
                    ELSIF TG_OP = 'DELETE' THEN
                      SELECT DISTINCT run_id
                      INTO   runId
                      FROM   old_table;
                    ELSE
                      SELECT DISTINCT run_id
                      INTO   runId
                      FROM   new_table;
                    END IF;
                    PERFORM pg_notify(
                      'pipelineruntasks',
                      runId::text
                    );
                    RETURN NULL;
                END;
                ${'$'}BODY${'$'};
            """.trimIndent()
        ),
        Trigger(
            trigger = """
                CREATE TRIGGER update_event
                    AFTER UPDATE
                    ON public.pipeline_run_tasks
                    REFERENCING NEW TABLE AS new_table OLD TABLE AS old_table
                    FOR EACH STATEMENT
                    EXECUTE FUNCTION public.pipeline_run_tasks_event();
            """.trimIndent(),
            triggerFunction = """
                CREATE OR REPLACE FUNCTION public.pipeline_run_tasks_event()
                    RETURNS trigger
                    LANGUAGE 'plpgsql'
                    COST 100
                    VOLATILE NOT LEAKPROOF
                AS ${'$'}BODY${'$'}
                DECLARE
                    runId bigint;
                BEGIN
                    IF TG_OP = 'INSERT' THEN
                      SELECT DISTINCT run_id
                      INTO   runId
                      FROM   new_table;
                    ELSIF TG_OP = 'DELETE' THEN
                      SELECT DISTINCT run_id
                      INTO   runId
                      FROM   old_table;
                    ELSE
                      SELECT DISTINCT run_id
                      INTO   runId
                      FROM   new_table;
                    END IF;
                    PERFORM pg_notify(
                      'pipelineruntasks',
                      runId::text
                    );
                    RETURN NULL;
                END;
                ${'$'}BODY${'$'};
            """.trimIndent()
        ),
    )

    private fun checkTaskListState(connection: Connection, runId: Long) {
        val (runningId, taskStatus) = connection.queryFirstOrNull<Pair<Long, String>>(
            sql = """
                SELECT pr_task_id, task_status
                FROM   $tableName
                WHERE  run_id = ?
                AND    task_status in (?,?,?)
            """.trimIndent(),
            runId,
            TaskStatus.Running,
            TaskStatus.Scheduled,
            TaskStatus.Failed,
        ) ?: return
        when (TaskStatus.fromString(taskStatus)) {
            TaskStatus.Scheduled, TaskStatus.Running -> throw TaskRunningException(runningId)
            TaskStatus.Failed -> throw TaskFailedError(runningId)
            else -> return
        }
    }

    private fun requireTaskNotRunningId(connection: Connection, pipelineRunTaskId: Long) {
        val runningId = connection.queryFirstOrNull<Long>(
            sql = """
                SELECT t1.pr_task_id
                FROM   $tableName t1
                JOIN   $tableName t2
                ON     t1.run_id = t2.run_id
                WHERE  t2.pr_task_id = ?
                AND    t1.task_status IN ('Running'::task_status,'Scheduled'::task_status)
                LIMIT  1
            """.trimIndent(),
            pipelineRunTaskId,
        )
        if (runningId != null) {
            throw TaskRunningException(runningId)
        }
    }

    /**
     * Locks to the current connection and returns the [PipelineRunTask] specified by the [pipelineRunTaskId]
     *
     * @throws IllegalArgumentException when the provided ID returns no records
     */
    fun getWithLock(connection: Connection, pipelineRunTaskId: Long): PipelineRunTask {
        return connection.queryFirstOrNull(
            sql = "${PipelineRunTask.sql} FOR UPDATE",
            pipelineRunTaskId
        ) ?: throw IllegalArgumentException("ID provided did not match a record in the database")
    }

    /** Returns the [PipelineRunTask] specified by the provided [pipelineRunTaskId] or null if no record can be found */
    @Suppress("UNUSED")
    fun getRecord(connection: Connection, pipelineRunTaskId: Long): PipelineRunTask? {
        return connection.queryFirstOrNull(
            sql = PipelineRunTask.sql,
            pipelineRunTaskId,
        )
    }

    /**
     * Returns [NextTask] instance representing the next runnable task for the given [runId]. Verifies that the
     * [userOid] has the ability to run tasks for this [runId].
     *
     * @throws TaskRunningException when another task in the run is currently active or scheduled
     * @throws IllegalArgumentException when the user is not able to run tasks for the run or the next task to run
     * cannot be found
     */
    fun getRecordForRun(connection: Connection, userOid: Long, runId: Long): NextTask {
        require(UserHasRun.checkUserRun(connection, userOid, runId)) {
            "User provided cannot run tasks for this pipeline run"
        }
        return getNextTask(connection, runId) ?: throw NoRecordFound(tableName, message = "Cannot find next task")
    }

    /**
     * Reset task to waiting state and deletes all children tasks using a stored procedure
     *
     * @throws TaskRunningException when another task in the run is currently active or scheduled
     * @throws NoRecordAffected various cases can throw this exception. These include:
     * - the user is not able to run tasks for the run
     * - there is no record with the [pipelineRunTaskId] specified
     * - the record obtained is waiting to be scheduled
     */
    fun resetRecord(connection: Connection, userOid: Long, pipelineRunTaskId: Long) {
        requireTaskNotRunningId(connection, pipelineRunTaskId)
        val runId = getRecord(connection, pipelineRunTaskId)?.runId
            ?: throw NoRecordFound(tableName, "Could not find record for pr_task_id = $pipelineRunTaskId")
        UserHasRun.requireUserRun(connection, userOid, runId)
        ResetTask.call(connection, pipelineRunTaskId)
    }

    private val emptyLambda: DIV.() -> Unit = {}

    /**
     * Adds the desired generic task as the last child of the [parentTaskId]
     */
    fun addTask(
        connection: Connection,
        parentTaskId: Long,
        taskId: Long,
        modalContent: DIV.() -> Unit = emptyLambda,
    ): Long {
        val modal = if (modalContent !== emptyLambda) {
            StringBuilder().appendHTML().div {
                modalContent()
            }.toString()
        } else null
        val sql = """
            INSERT INTO $tableName(run_id,task_status,task_id,parent_task_id,parent_task_order,workflow_operation,
                                   modal_html) 
            SELECT distinct t1.run_id, 'Waiting'::task_status, ?, t1.pr_task_id,
                   COALESCE(
                      MAX(t2.parent_task_order) OVER (PARTITION BY t2.parent_task_id ) + 1,
                      1
                    ),
                   t1.workflow_operation,
                   ?
            FROM   $tableName t1
            LEFT JOIN $tableName t2 on t1.pr_task_id = t2.parent_task_id
            WHERE  t1.pr_task_id = ?
            RETURNING pr_task_id
        """.trimIndent()
        return connection.runReturningFirstOrNull(
            sql = sql,
            taskId,
            modal,
            parentTaskId,
        ) ?: throw NoRecordAffected(tableName, "Did not insert a record as a child task to id = $parentTaskId")
    }

    /**
     * Returns all the tasks associated with the provided [runId], ordered by the parent child relationships and
     * relative ordering within those relationships
     */
    private fun getOrderedTasks(connection: Connection, runId: Long): List<ResponsePrTask> {
        return GetTasksOrdered.getTasks(connection, runId)
    }

    /**
     * Returns all the tasks associated with the provided [runId], ordered by the parent child relationships and
     * relative ordering within those relationships
     */
    fun getOrderedTasksNew(connection: Connection, runId: Long): List<ResponsePrTask> {
        return connection.submitQuery(
            sql = """
                with t1 as (
                    select tasks.*,
                           sum(case
                                when coalesce(last_parent_id,0) > parent_task_id then -1
                                when coalesce(last_parent_id,0) = parent_task_id then 0
                                else 1
                               end)
                           over (rows unbounded preceding) current_level
                    from  (select tasks.*,
                                  lag(parent_task_id) over () last_parent_id
                           from   ${GetTasksOrdered.name}(?) tasks) tasks
                )
                select task_order, pr_task_id, run_id, task_start, task_completed, task_id, task_message, task_status,
                       parent_task_id, parent_task_order, workflow_operation, task_stack_trace, modal_html,
                       ltrim(repeat('--'::text,current_level::int)||'> '||task_name,'> ') task_name,
                       task_description,
                       task_run_type
                from   t1;
            """.trimIndent(),
            runId,
        )
    }

    /**
     * Returns the next runnable task for the given [runId] as a [NextTask]
     *
     * @throws TaskRunningException when another task in the run is currently active or scheduled
     * @throws IllegalArgumentException when a task in the run is currently running or scheduled
     */
    fun getNextTask(connection: Connection, runId: Long): NextTask? {
        checkTaskListState(connection, runId)
        return getOrderedTasks(connection, runId).firstOrNull {
            it.taskStatus.isTaskStatus<TaskStatus.Waiting>()
        }?.let { record ->
            NextTask(
                record.pipelineRunTaskId,
                record.taskId,
                TaskRunType.fromString(record.taskRunType),
            )
        }
    }

    /**
     * Updates the status for the given record
     */
    fun setStatus(connection: Connection, pipelineRunTaskId: Long, status: TaskStatus) {
        connection.runUpdate(
            sql = "UPDATE $tableName SET task_status = ? WHERE pr_Task_id = ?",
            status,
            pipelineRunTaskId,
        )
    }

    private object NonUpdatedField

    /**
     * Generic update where some fields can be excluded from the function call and the update will not include them.
     * Uses empty singleton to do referential checking to see which fields were provided for update.
     */
    @Suppress("LongParameterList", "ComplexMethod")
    fun update(
        connection: Connection,
        pipelineRunTaskId: Long,
        taskStatus: Any = NonUpdatedField,
        taskStart: Any? = NonUpdatedField,
        taskCompleted: Any? = NonUpdatedField,
        taskMessage: Any? = NonUpdatedField,
        taskStackTrace: Any? = NonUpdatedField,
    ) {
        val updates = mutableMapOf<String, Any?>()
        if (taskStatus !== NonUpdatedField) {
            updates["task_status"] = if (taskStatus is TaskStatus) {
                taskStatus
            } else {
                error("taskStatus must be a TaskStatus")
            }
        }
        if (taskStart !== NonUpdatedField) {
            updates["task_start"] = when (taskStart) {
                is Timestamp? -> taskStart
                is Instant? -> taskStart?.let { Timestamp(it.toEpochMilli()) }
                else -> error("taskStart must be an Instant or Timestamp")
            }
        }
        if (taskCompleted !== NonUpdatedField) {
            updates["task_completed"] = when (taskCompleted) {
                is Timestamp? -> taskCompleted
                is Instant? -> taskCompleted?.let { Timestamp(it.toEpochMilli()) }
                else -> error("taskCompleted must be an Instant or Timestamp")
            }
        }
        if (taskMessage !== NonUpdatedField) {
            updates["task_message"] = if (taskMessage is String?) {
                taskMessage
            } else {
                error("taskMessage must be a String")
            }
        }
        if (taskStackTrace !== NonUpdatedField) {
            updates["task_stack_trace"] = if (taskStackTrace is String?) {
                taskStackTrace
            } else {
                error("taskStackTrace must be a String")
            }
        }
        val sortedMap = updates.toSortedMap()
        val sql = """
            UPDATE $tableName
            SET    ${sortedMap.entries.joinToString { "${it.key} = ?" }}
            WHERE  pr_task_id = ?
        """.trimIndent()
        connection.runUpdate(
            sql = sql,
            sortedMap.values,
            pipelineRunTaskId,
        )
    }
}
