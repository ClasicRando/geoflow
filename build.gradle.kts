plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
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
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }
}
