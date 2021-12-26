package me.geoflow.core.database.functions

import kotlin.reflect.typeOf

/**
 * DB function for getting all operations available to a give user. No public API for calling within the codebase.
 * Currently, used within queries and existing here for build script purposes.
 */
object GetUserOperations: PlPgSqlTableFunction(
    name = "get_user_operation",
    parameterTypes = listOf(
        typeOf<Long>(),
    ),
) {
    override val functionCode: String = """
        CREATE FUNCTION public.get_user_operation(IN user_oid bigint)
            RETURNS setof workflow_operations
            LANGUAGE 'plpgsql'
            
        AS ${'$'}BODY${'$'}
        declare
            v_roles text[];
        begin
            select t1.roles into v_roles
            from   internal_users t1
        	where  t1.user_oid = $1;
            
            if 'admin' = ANY(v_roles) then
        		return query select * from workflow_operations ORDER BY workflow_order;
        	else
        		return query select * from workflow_operations where role = ANY(v_roles) ORDER BY workflow_order;
        	end if;
        end;
        ${'$'}BODY${'$'};
    """.trimIndent()

    override val innerFunctions: List<String> = emptyList()
}
