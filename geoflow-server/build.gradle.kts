import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("plugin.serialization") version "1.5.21"
}

version = "0.1"
val ktormVersion = "3.4.1"
val ktorVersion = "1.6.3"
val htmlJvmVersion = "0.7.3"
val slfVersion = "1.7.32"
val postgresqlVersion = "42.2.23"

dependencies {
    implementation(project(":geoflow-core"))
    testImplementation(kotlin("test", "1.5.21"))
    // https://mvnrepository.com/artifact/org.ktorm/ktorm-core
    implementation("org.ktorm:ktorm-core:$ktormVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
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