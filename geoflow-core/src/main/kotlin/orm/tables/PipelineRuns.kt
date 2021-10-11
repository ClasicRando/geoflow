package orm.tables

import database.DatabaseConnection
import formatLocalDateDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.ktorm.dsl.*
import org.ktorm.jackson.json
import org.ktorm.schema.*
import org.ktorm.support.postgresql.LockingMode
import org.ktorm.support.postgresql.locking
import orm.enums.OperationState
import orm.entities.PipelineRun
import orm.entities.PipelineRunTask
import orm.enums.MergeType
import kotlin.jvm.Throws

object PipelineRuns: DbTable<PipelineRun>("pipeline_runs") {
    val runId = long("run_id").primaryKey().bindTo { it.runId }
    val dsId = long("ds_id").references(DataSources) { it.dataSource }
    val recordDate = date("record_date").bindTo { it.recordDate }
    val workflowOperation = text("workflow_operation").bindTo { it.workflowOperation }
    val operationState = enum<OperationState>("operation_state").bindTo { it.operationState }
    val collectionUser = long("collection_user_oid").references(InternalUsers) { it.collectionUser }
    val loadUser = long("load_user_oid").references(InternalUsers) { it.loadUser }
    val checkUser = long("check_user_oid").references(InternalUsers) { it.checkUser }
    val qaUser = long("qa_user_oid").references(InternalUsers) { it.qaUser }
    val productionCount = int("production_count").bindTo { it.productionCount }
    val stagingCount = int("staging_count").bindTo { it.stagingCount }
    val matchCount = int("match_count").bindTo { it.matchCount }
    val newCount = int("new_count").bindTo { it.newCount }
    val plottingStats = json<Map<String, Int>>("plotting_stats").bindTo { it.plottingStats }
    val hasChildTables = boolean("has_child_tables").bindTo { it.hasChildTables }
    val mergeType = enum<MergeType>("merge_type").bindTo { it.mergeType }

    val tableDisplayFields = mapOf(
        "ds_id" to mapOf("name" to "Data Source ID"),
        "ds_code" to mapOf("name" to "Data Source Code"),
        "record_date" to mapOf(),
        "operation_state" to mapOf(),
        "collection_user" to mapOf(),
        "load_user" to mapOf(),
        "check_user" to mapOf(),
        "qa_user" to mapOf("name" to "QA User"),
    )

    val createSequence = """
        CREATE SEQUENCE public.pipeline_runs_run_id_seq
            INCREMENT 1
            START 1
            MINVALUE 1
            MAXVALUE 9223372036854775807
            CACHE 1;
    """.trimIndent()

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.pipeline_runs
        (
            ds_id bigint NOT NULL,
            record_date date NOT NULL,
            load_user_oid bigint,
            check_user_oid bigint,
            qa_user_oid bigint,
            collection_user_oid bigint,
            workflow_operation text COLLATE pg_catalog."default" NOT NULL,
            run_id bigint NOT NULL DEFAULT nextval('pipeline_runs_run_id_seq'::regclass),
            operation_state operation_state NOT NULL,
            production_count integer NOT NULL,
            staging_count integer NOT NULL,
            match_count integer NOT NULL,
            new_count integer NOT NULL,
            plotting_stats jsonb NOT NULL,
            has_child_tables boolean NOT NULL,
            merge_type merge_type,
            CONSTRAINT pipeline_runs_pkey PRIMARY KEY (run_id),
            CONSTRAINT ds_id FOREIGN KEY (ds_id)
                REFERENCES public.data_sources (ds_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE
                NOT VALID
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    // Trigger function for updating a pipeline run record
    val updatePipelineRun = """
        CREATE OR REPLACE FUNCTION public.update_pipeline_run()
            RETURNS trigger
            LANGUAGE 'plpgsql'
            COST 100
            VOLATILE NOT LEAKPROOF
        AS ${'$'}BODY${'$'}
        DECLARE
            v_pipeline_id BIGINT;
        BEGIN
            SELECT CASE NEW.workflow_operation
                    WHEN 'collection' THEN collection_pipeline
                    WHEN 'load' THEN load_pipeline
                    WHEN 'check' THEN check_pipeline
                    WHEN 'qa' THEN qa_pipeline
                   END
            INTO   v_pipeline_id
            FROM   data_sources
            WHERE  ds_id = NEW.ds_id;
            IF NEW.operation_state = 'Active' AND OLD.operation_state = 'Ready' THEN
                INSERT INTO pipeline_run_tasks(run_id,task_id,parent_task_id,parent_task_order)
                SELECT NEW.run_id, t2.task_id, t2.parent_task, t2.parent_task_order
                FROM   pipelines t1
                JOIN   pipeline_tasks t2
                ON	   t1.pipeline_id = t2.pipeline_id
                WHERE  t1.pipeline_id = v_pipeline_id;
            END IF;
            RETURN NEW;
        END;
        ${'$'}BODY${'$'};
    """.trimIndent()

    val updateTrigger = """
        CREATE TRIGGER update_record
            BEFORE UPDATE 
            ON public.pipeline_runs
            FOR EACH ROW
            EXECUTE FUNCTION public.update_pipeline_run();
    """.trimIndent()

    fun checkUserRun(runId: Long, username: String): Boolean {
        val user = InternalUsers.getUser(username)
        if ("admin" in user.roles) {
            return true
        }
        val runUserIds = getRun(runId)?.let { run ->
            listOf(
                run.collectionUser?.userOid,
                run.loadUser?.userOid,
                run.checkUser?.userOid,
                run.qaUser?.userOid,
            ).mapNotNull { it }
        } ?: listOf()
        return user.userOid in runUserIds
    }

    @Serializable
    data class Record(
        @SerialName("run_id")
        val runId: Long,
        @SerialName("ds_id")
        val dsId: Long,
        @SerialName("ds_code")
        val dsCode: String,
        @SerialName("record_date")
        val recordDate: String,
        @SerialName("operation_state")
        val operationState: String,
        @SerialName("collection_user")
        val collectionUser: String,
        @SerialName("load_user")
        val loadUser: String,
        @SerialName("check_user")
        val checkUser: String,
        @SerialName("qa_user")
        val qaUser: String)

    @Throws(IllegalArgumentException::class)
    fun userRuns(userId: Long, state: String): List<Record> {
        val columnCheck = when(state) {
            "collection" -> collectionUser
            "load" -> loadUser
            "check" -> checkUser
            "qa" -> qaUser
            else -> throw IllegalArgumentException("state provided does not point to anything")
        }
        return DatabaseConnection
            .database
            .from(this)
            .joinReferencesAndSelect()
            .where((columnCheck eq userId) or columnCheck.isNull())
            .map(this::createEntity)
            .map { run ->
                Record(
                    run.runId,
                    run.dataSource.dsId,
                    run.dataSource.code,
                    formatLocalDateDefault(run.recordDate),
                    run.operationState.name,
                    run.collectionUser?.name ?: "",
                    run.loadUser?.name ?: "",
                    run.checkUser?.name ?: "",
                    run.qaUser?.name ?: ""
                )
            }
    }

    fun lastRun(pipelineRunTask: PipelineRunTask): Long? {
        val dsId = DatabaseConnection
            .database
            .from(this)
            .select(dsId)
            .where(runId eq pipelineRunTask.runId)
            .map { row -> row[dsId] ?: 0 }
            .first()
        return DatabaseConnection
            .database
            .from(this)
            .select(runId)
            .where(this.dsId eq dsId)
            .orderBy(runId.desc())
            .limit(1, 1)
            .map { row -> row[runId] }
            .firstOrNull()
    }

    fun getRun(runId: Long): PipelineRun? {
        return DatabaseConnection
            .database
            .from(this)
            .joinReferencesAndSelect()
            .where(this.runId eq runId)
            .map(this::createEntity)
            .firstOrNull()
    }

    @Throws(NoSuchElementException::class)
    private fun reserveRecord(runId: Long): PipelineRun {
        return DatabaseConnection
            .database
            .from(this)
            .select()
            .locking(LockingMode.FOR_SHARE)
            .where(this.runId eq runId)
            .map(this::createEntity)
            .first()
    }

    @Throws(NoSuchElementException::class, IllegalArgumentException::class)
    fun pickupRun(runId: Long, userId: Long) {
        DatabaseConnection.database.run {
            useTransaction {
                val run = reserveRecord(runId)
                val updateColumn = when(run.workflowOperation) {
                    "collection" -> collectionUser
                    "load" -> loadUser
                    "check" -> checkUser
                    "qa" -> qaUser
                    else -> throw IllegalArgumentException("Run's workflow operation is not valid")
                }
                update(PipelineRuns) {
                    set(operationState, OperationState.Active)
                    set(updateColumn, userId)
                    where { PipelineRuns.runId eq runId }
                }
            }
        }
    }
}