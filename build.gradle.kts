plugins {
    kotlin("jvm") version "1.5.21"
    kotlin("plugin.serialization") version "1.5.21"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "me.steven"
version = "0.1"

allprojects {
    repositories {
        mavenCentral()
        jcenter()
    }
}