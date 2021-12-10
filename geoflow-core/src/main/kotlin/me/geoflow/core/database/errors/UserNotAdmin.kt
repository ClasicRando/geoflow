package me.geoflow.core.database.errors

/** Exception thrown when the user is required to be an admin for an operation, but does not have the role */
class UserNotAdmin : Throwable("API action requires admin role and the user ID provided does not meet that requirement")
