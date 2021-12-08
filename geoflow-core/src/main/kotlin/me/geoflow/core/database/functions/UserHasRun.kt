package me.geoflow.core.database.functions

import java.sql.Connection
import kotlin.reflect.typeOf

/**
 * Definition for plpgsql function that returns true if the user ID provided has the ability to operate on the specified
 * [run][me.geoflow.core.database.tables.PipelineRunTasks]
 */
object UserHasRun: PlPgSqlFunction(
    name = "user_has_run",
    parameterTypes = listOf(
        typeOf<Long>(),
        typeOf<Long>(),
    ),
) {

    /**
     * Call underlining plpgsql function execution code to validate if the provided [userOid] has the ability to execute
     * operations on the specified [runId]
     */
    fun checkUserRun(connection: Connection, userOid: Long, runId: Long): Boolean {
        return call(connection, userOid, runId)
    }

    override val functionCode: String = """
        CREATE OR REPLACE FUNCTION public.user_has_run(
        	user_oid bigint,
        	run_id bigint)
            RETURNS boolean
            LANGUAGE 'plpgsql'
            COST 100
            VOLATILE PARALLEL UNSAFE
        AS ${'$'}BODY${'$'}
        declare
            r_user internal_users;
            r_run pipeline_runs;
        begin
            select * into r_user
            from   internal_users
            where  internal_users.user_oid = $1;
            
            if 'admin' = ANY(r_user.roles) then
                return true;
            end if;
            
            select * into r_run
            from   pipeline_runs
            where  pipeline_runs.run_id = $2;
            
            return $1 in (r_run.collection_user_oid,r_run.load_user_oid,r_run.check_user_oid,r_run.qa_user_oid);
        end;
        ${'$'}BODY${'$'};
    """.trimIndent()

    override val innerFunctions: List<String> = emptyList()
}
