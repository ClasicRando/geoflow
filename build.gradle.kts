plugins {
    kotlin("jvm") version "1.5.21" apply false
    id("com.github.johnrengelman.shadow") version "7.0.0" apply false
}

version = "0.1"

allprojects {
    repositories {
        mavenCentral()
        jcenter()
    }
    group = "me.steven"
}

subprojects {
    apply(plugin="kotlin")
    apply(plugin="com.github.johnrengelman.shadow")

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}