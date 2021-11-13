package database.functions

object Constraints {

    /** Function used to verify a text field is not empty or containing only whitespace. Returns true for null values */
    val checkBlankOrNull = """
        CREATE OR REPLACE FUNCTION public.check_blank_or_empty(
        	"textInput" text)
            RETURNS boolean
            LANGUAGE 'plpgsql'
            COST 100
            VOLATILE PARALLEL UNSAFE
        AS         ${'$'}BODY${'$'}
        begin
        return coalesce($1,'x') ~ '^\s*$';
        end;
        ${'$'}BODY${'$'};
    """.trimIndent()
}