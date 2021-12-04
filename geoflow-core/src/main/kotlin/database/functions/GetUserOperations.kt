package database.functions

import kotlin.reflect.typeOf

/** */
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
        		return query select * from workflow_operations;
        	else
        		return query select * from workflow_operations where role = ANY(v_roles);
        	end if;
        end;
        ${'$'}BODY${'$'};
    """.trimIndent()

    override val innerFunctions: List<String> = emptyList()
}
