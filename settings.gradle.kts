pluginManagement {
    val kotlinVersion: String by settings
    val shadowJarVersion: String by settings
    plugins {
        kotlin("jvm") version kotlinVersion
        id("com.github.johnrengelman.shadow") version shadowJarVersion
    }
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "geoflow"

include(":geoflow-core")
include(":geoflow-server")
include(":geoflow-worker")
include(":geoflow-api")
