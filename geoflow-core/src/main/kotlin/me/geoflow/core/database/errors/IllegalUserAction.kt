package me.geoflow.core.database.errors

/** Exception thrown when the user makes a request that is not allowed based upon access or role */
class IllegalUserAction(message: String) : Throwable(message)
