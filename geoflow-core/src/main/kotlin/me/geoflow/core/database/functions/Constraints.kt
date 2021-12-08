package me.geoflow.core.database.functions

/**
 * Storage for global check constraint functions. Should only be used by the BuildScript
 */
object Constraints {

    /**
     * List of create statements for functions used in global constraint checks
     */
    val functions: List<String> = listOf(
        /**
         * Returns true if the value passed is null or text containing characters other than whitespace
         */
        """
        CREATE OR REPLACE FUNCTION public.check_not_blank_or_empty(
            text)
            RETURNS boolean
            LANGUAGE 'plpgsql'
            COST 100
            VOLATILE PARALLEL UNSAFE
        AS ${'$'}BODY${'$'}
        BEGIN
            RETURN COALESCE($1,'x') !~ '^\s*$';
        END;
        ${'$'}BODY${'$'};
        """.trimIndent(),
        /**
         * Returns true if all values passed in the array are null or text containing characters other than whitespace.
         * Returns false if the array passed is empty.
         */
        """
        CREATE OR REPLACE FUNCTION public.check_array_not_blank_or_empty(
        	text[])
            RETURNS boolean
            LANGUAGE 'plpgsql'
            COST 100
            VOLATILE PARALLEL UNSAFE
        AS ${'$'}BODY${'$'}
        DECLARE
        	val text;
        BEGIN
        	IF array_length($1, 1) = 0 THEN
        		RETURN false;
        	END IF;
        	FOREACH val IN ARRAY $1
        	LOOP
        		IF COALESCE(val,'x') ~ '^\s*$' THEN
        			RETURN false;
        		END IF;
        	END LOOP;
        	RETURN true;
        END;
        ${'$'}BODY${'$'};
    """.trimIndent()
    )
}
