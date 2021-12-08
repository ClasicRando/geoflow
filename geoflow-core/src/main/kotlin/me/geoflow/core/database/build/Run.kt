package me.geoflow.core.database.build

import me.geoflow.core.database.Database

/** Entry point to building the database */
fun main() {
    Database.runWithConnectionBlocking {
        BuildScript.buildDatabase(it)
    }
}
