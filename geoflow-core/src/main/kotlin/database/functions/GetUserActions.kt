package database.functions

import kotlin.reflect.typeOf

/** */
object GetUserActions: PlPgSqlTableFunction(
    name = "get_user_actions",
    parameterTypes = listOf(
        typeOf<Long>(),
    ),
) {
    override val functionCode: String = """
        CREATE FUNCTION public.get_user_actions(IN user_oid bigint)
            RETURNS setof actions
            LANGUAGE 'plpgsql'
            
        AS ${'$'}BODY${'$'}
        declare
            v_roles text[];
        begin
            select t1.roles into v_roles
            from   internal_users t1
        	where  t1.user_oid = $1;
            
            if 'admin' = ANY(v_roles) then
        		return query select * from actions;
        	else
        		return query select * from actions where role = ANY(v_roles);
        	end if;
        end;
        ${'$'}BODY${'$'};
    """.trimIndent()

    override val innerFunctions: List<String> = emptyList()
}
