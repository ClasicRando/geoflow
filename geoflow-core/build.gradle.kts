plugins {
    kotlin("plugin.serialization") version "1.5.30"
}

version = "0.1"
val ktormVersion: String by project
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

dependencies {
    testImplementation(kotlin("test", "1.5.30"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxJsonVersion")
    // https://mvnrepository.com/artifact/org.ktorm/ktorm-core
    implementation("org.ktorm:ktorm-core:$ktormVersion")
    // https://mvnrepository.com/artifact/org.ktorm/ktorm-jackson
    implementation("org.ktorm:ktorm-jackson:$ktormVersion")
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
    implementation("com.beust:klaxon:$klaxonVersion")
    // https://mvnrepository.com/artifact/at.favre.lib/bcrypt
    implementation("at.favre.lib:bcrypt:$bcryptVersion")

}

tasks.test {
    useJUnitPlatform()
}