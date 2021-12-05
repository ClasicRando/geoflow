package database.build

import database.Database

/** Entry point to building the database */
fun main() {
    Database.runWithConnectionBlocking {
        BuildScript.buildDatabase(it)
    }
}
