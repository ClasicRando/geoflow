import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.21"
    application
    kotlin("plugin.serialization") version "1.5.21"
}

group = "me.steven"
version = "1.0-SNAPSHOT"
val ktorVersion = "1.6.3"
val htmlJvmVersion = "0.7.3"

dependencies {
    implementation(project(":geoflow-core"))
    testImplementation(kotlin("test", "1.5.21"))
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-html-builder:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$htmlJvmVersion")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("ServerKt")
}