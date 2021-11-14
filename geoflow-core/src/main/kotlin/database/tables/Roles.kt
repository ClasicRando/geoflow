package database.tables

/**
 * Table used to store the available roles that a user can hold
 */
object Roles: DbTable("roles") {

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.roles
        (
            name text PRIMARY KEY COLLATE pg_catalog."default" CHECK (check_not_blank_or_empty(name)),
            description text COLLATE pg_catalog."default" NOT NULL CHECK (check_not_blank_or_empty(description))
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}