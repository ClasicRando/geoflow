plugins {
    application
}

version = "0.1"
val kotlinVersion: String by project
val kjobVersion: String by project
val coroutinesVersion: String by project
val slf4Version: String by project
val kotlinLoggingVersion: String by project
val ktormVersion: String by project

dependencies {
    implementation(project(":geoflow-core"))
    testImplementation(kotlin("test", "1.5.30"))
    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    // https://mvnrepository.com/artifact/it.justwrote/kjob-core
    implementation("it.justwrote:kjob-core:$kjobVersion")
    // https://mvnrepository.com/artifact/it.justwrote/kjob-mongo
    implementation("it.justwrote:kjob-mongo:$kjobVersion")
    // https://mvnrepository.com/artifact/it.justwrote/kjob-inmem
    implementation("it.justwrote:kjob-inmem:$kjobVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:$slf4Version")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
    implementation("org.slf4j:slf4j-simple:$slf4Version")
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")
    // https://mvnrepository.com/artifact/org.ktorm/ktorm-core
    implementation("org.ktorm:ktorm-core:$ktormVersion")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt")
}