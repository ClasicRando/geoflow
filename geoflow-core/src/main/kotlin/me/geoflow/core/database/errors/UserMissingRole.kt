package me.geoflow.core.database.errors

/** Exception thrown when a user does not meet a role requirement */
class UserMissingRole(role: String) : Throwable("User is missing the $role role")
