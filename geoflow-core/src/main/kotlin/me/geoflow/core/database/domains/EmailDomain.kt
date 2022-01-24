package me.geoflow.core.database.domains

/**
 * Domain for email format specification. Use to make sure a column is populated by email addresses. Does not enforce a
 * NOT NULL constraint
 */
object EmailDomain : Domain(
    name = "email_address",
    dataType = "citext",
    "email_address_check" to "VALUE ~ '^[a-zA-Z0-9.!#\$%&''*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]"
    + "{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\$'::citext OR VALUE IS NULL",
)
