import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
}

group = "me.steven"
version = "0.1"
val ktorVersion = "1.6.3"
val htmlJvmVersion = "0.7.3"
val slfVersion = "1.7.32"

dependencies {
    implementation(project(":geoflow-core"))
    testImplementation(kotlin("test", "1.5.21"))
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-html-builder:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$htmlJvmVersion")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:$slfVersion")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-simple:$slfVersion")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("ServerKt")
}

tasks{
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", application.mainClass))
        }
    }
}