package me.geoflow.core.database.tables

import me.geoflow.core.database.errors.NoRecordAffected
import me.geoflow.core.database.extensions.queryHasResult
import me.geoflow.core.database.extensions.runReturningFirstOrNull
import me.geoflow.core.database.extensions.runUpdate
import java.sql.Connection

/** */
object PipelineRunTaskRules : DbTable("pipeline_run_task_rules"), ApiExposed {

    override val createStatement: String = """
        CREATE TABLE IF NOT EXISTS public.pipeline_run_task_rules
        (
            pr_task_rule_id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
            pr_task_id bigint NOT NULL REFERENCES public.pipeline_run_tasks (pr_task_id) MATCH SIMPLE
                ON UPDATE CASCADE
                ON DELETE CASCADE,
			rule_name text NOT NULL COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(rule_name)),
			is_failed boolean NOT NULL DEFAULT FALSE,
			rule_message text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(rule_message)),
			rule_description text COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(rule_description))
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()

    override val tableDisplayFields: Map<String, Map<String, String>> = mapOf(
        "rule_name" to mapOf("title" to "Name"),
        "is_failed" to mapOf("title" to "Is Failed?"),
        "rule_message" to mapOf("title" to "Message"),
    )

    /** */
    fun createRule(
        connection: Connection,
        pipelineRunTaskId: Long,
        ruleName: String,
        description: String? = null,
    ): Long {
        return connection.runReturningFirstOrNull<Long>(
            sql = """
                INSERT INTO pipeline_run_task_rules(pr_task_id,rule_name,rule_description)
                VALUES(?,?,?)
                RETURNING pr_task_rule_id
            """.trimIndent(),
            pipelineRunTaskId,
            ruleName,
            description,
        ) ?: throw NoRecordAffected(tableName, "New pr_task_rule_id was not returned")
    }

    /** */
    fun setRuleFailed(connection: Connection, prTaskRuleId: Long, message: String) {
        connection.runUpdate(
            sql = """
                UPDATE $tableName
                SET    is_failed = TRUE,
                       rule_message = ?
                WHERE  pr_task_rule_id = ?
            """.trimIndent(),
            message,
            prTaskRuleId,
        )
    }

    /** */
    fun setMessage(connection: Connection, prTaskRuleId: Long, message: String) {
        connection.runUpdate(
            sql = """
                UPDATE $tableName
                SET    rule_message = ?
                WHERE  pr_task_rule_id = ?
            """.trimIndent(),
            message,
            prTaskRuleId,
        )
    }

    /** */
    fun appendToMessage(connection: Connection, prTaskRuleId: Long, message: String) {
        connection.runUpdate(
            sql = """
                UPDATE $tableName
                SET    rule_message = rule_message||?
                WHERE  pr_task_rule_id = ?
            """.trimIndent(),
            message,
            prTaskRuleId,
        )
    }

    /** */
    fun putLine(connection: Connection, prTaskRuleId: Long, message: String) {
        connection.runUpdate(
            sql = """
                UPDATE $tableName
                SET    rule_message = rule_message||CHR(10)||?
                WHERE  pr_task_rule_id = ?
            """.trimIndent(),
            message,
            prTaskRuleId,
        )
    }

    /** */
    fun hasBrokenRule(connection: Connection, pipelineRunTaskId: Long): Boolean {
        return connection.queryHasResult(
            sql = "SELECT * FROM $tableName WHERE pr_task_id = ? AND is_failed",
            pipelineRunTaskId
        )
    }

}
