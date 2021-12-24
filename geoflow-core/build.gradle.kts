@file:Suppress("KDocMissingDocumentation")
plugins {
    kotlin("plugin.serialization")
}

version = "0.1"
val kotlinVersion: String by project
val postgresqlVersion: String by project
val kjobVersion: String by project
val dbfVersion: String by project
val ucanaccessVersion: String by project
val dbcpVersion: String by project
val univocityVersion: String by project
val poiVersion: String by project
val klaxonVersion: String by project
val bcryptVersion: String by project
val kotlinxJsonVersion: String by project
val ktorVersion: String by project
val junitVersion: String by project
val slf4Version: String by project
val kotlinLoggingVersion: String by project
val reflectionsVersion: String by project
val coroutinesJdbc: String by project
val kMongoVersion: String by project
val log4jVersion: String by project

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-json-jvm
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxJsonVersion")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    // https://mvnrepository.com/artifact/org.postgresql/postgresql
    implementation("org.postgresql","postgresql", postgresqlVersion)
    // https://mvnrepository.com/artifact/it.justwrote/kjob-core
    implementation("it.justwrote:kjob-core:$kjobVersion")
    // https://mvnrepository.com/artifact/com.github.albfernandez/javadbf
    implementation("com.github.albfernandez:javadbf:$dbfVersion")
    // https://mvnrepository.com/artifact/net.sf.ucanaccess/ucanaccess
    implementation("net.sf.ucanaccess:ucanaccess:$ucanaccessVersion")
    // https://mvnrepository.com/artifact/org.apache.commons/commons-dbcp2
    implementation("org.apache.commons:commons-dbcp2:$dbcpVersion")
    // https://mvnrepository.com/artifact/com.univocity/univocity-parsers
    implementation("com.univocity:univocity-parsers:$univocityVersion")
    // https://mvnrepository.com/artifact/org.apache.poi/poi
    implementation("org.apache.poi:poi:$poiVersion")
    // https://mvnrepository.com/artifact/org.apache.poi/poi-ooxml
    implementation("org.apache.poi:poi-ooxml:$poiVersion")
    implementation("com.beust:klaxon:$klaxonVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization:$ktorVersion")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:$slf4Version")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
    implementation("org.slf4j:slf4j-simple:$slf4Version")
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")
    implementation("org.reflections:reflections:$reflectionsVersion")
    implementation("com.michael-bull.kotlin-coroutines-jdbc:kotlin-coroutines-jdbc:$coroutinesJdbc")
    implementation("org.litote.kmongo:kmongo-coroutine:$kMongoVersion")
    implementation("org.litote.kmongo:kmongo-coroutine-serialization:$kMongoVersion")
    implementation("org.litote.kmongo:kmongo-id-serialization:$kMongoVersion")
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

tasks.test {
    testLogging {
        setExceptionFormat("full")
        events = setOf(
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
        )
        showStandardStreams = true
        afterSuite(KotlinClosure2<TestDescriptor,TestResult,Unit>({ descriptor, result ->
            if (descriptor.parent == null) {
                println("\nTest Result: ${result.resultType}")
                println("""
                    Test summary: ${result.testCount} tests, 
                    ${result.successfulTestCount} succeeded, 
                    ${result.failedTestCount} failed, 
                    ${result.skippedTestCount} skipped
                """.trimIndent().replace("\n", ""))
            }
        }))
    }
    useJUnitPlatform()
}
