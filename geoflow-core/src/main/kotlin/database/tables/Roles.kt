package database.tables

/**
 * Table used to store the available roles that a user can hold
 */
object Roles: DbTable("roles") {

    override val createStatement = """
        CREATE TABLE IF NOT EXISTS public.roles
        (
            name text COLLATE pg_catalog."default" NOT NULL,
            description text COLLATE pg_catalog."default" NOT NULL,
            CONSTRAINT roles_pkey PRIMARY KEY (name)
        )
        WITH (
            OIDS = FALSE
        );
    """.trimIndent()
}