@file:Suppress("KDocMissingDocumentation")

plugins {
    application
    kotlin("plugin.serialization")
}

version = "0.1"
val kotlinVersion: String by project
val kjobVersion: String by project
val ktorVersion: String by project
val htmlJvmVersion: String by project
val slf4Version: String by project
val postgresqlVersion: String by project
val kotlinLoggingVersion: String by project
val reflectionsVersion: String by project
val log4jVersion: String by project

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib", kotlinVersion))
    implementation(project(":geoflow-core"))
    testImplementation(kotlin("test", kotlinVersion))
    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    // https://mvnrepository.com/artifact/it.justwrote/kjob-core
    implementation("it.justwrote:kjob-core:$kjobVersion")
    // https://mvnrepository.com/artifact/it.justwrote/kjob-mongo
    implementation("it.justwrote:kjob-mongo:$kjobVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-html-builder:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    implementation("io.ktor:ktor-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$htmlJvmVersion")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:$slf4Version")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
    implementation("org.slf4j:slf4j-simple:$slf4Version")
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")
    implementation("org.reflections:reflections:$reflectionsVersion")
    constraints {
        add("implementation", "org.apache.logging.log4j:log4j-core") {
            version {
                strictly("[$log4jVersion, 3[")
                prefer(log4jVersion)
            }
            because("CVE-2021-44228/45105/45046: Log4j vulnerable to remote code execution")
        }
        add("implementation", "org.apache.logging.log4j:log4j-api") {
            version {
                strictly("[$log4jVersion, 3[")
                prefer(log4jVersion)
            }
            because("CVE-2021-44228/45105/45046: Log4j vulnerable to remote code execution")
        }
    }
}

application {
    mainClass.set("ApiKt")
}

tasks{
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", application.mainClass))
        }
    }
}
