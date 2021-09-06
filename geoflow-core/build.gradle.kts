import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
}

version = "0.1"
val ktormVersion = "3.4.1"
val postgresqlVersion = "42.2.23.jre7"
val kjobVersion = "0.2.0"
val dbfVersion = "1.13.2"
val ucanaccessVersion = "5.0.1"
val dbcpVersion = "2.9.0"
val univocityVersion = "2.9.1"
val poiVersion = "5.0.0"
val serializationVersion = "5.5"

dependencies {
    testImplementation(kotlin("test", "1.5.21"))
    // https://mvnrepository.com/artifact/org.ktorm/ktorm-core
    implementation("org.ktorm:ktorm-core:$ktormVersion")
    // https://mvnrepository.com/artifact/org.ktorm/ktorm-support-postgresql
    implementation("org.ktorm:ktorm-support-postgresql:$ktormVersion")
    // https://mvnrepository.com/artifact/org.postgresql/postgresql
    implementation("org.postgresql:postgresql:$postgresqlVersion")
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
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-json-jvm
    implementation("com.beust:klaxon:$serializationVersion")
}

tasks.test {
    useJUnitPlatform()
}