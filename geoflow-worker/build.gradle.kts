import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
}

version = "0.1"
val kjobVersion = "0.2.0"

dependencies {
    implementation(project(":geoflow-core"))
    testImplementation(kotlin("test", "1.5.21"))
    // https://mvnrepository.com/artifact/it.justwrote/kjob-core
    implementation("it.justwrote:kjob-core:$kjobVersion")
    // https://mvnrepository.com/artifact/it.justwrote/kjob-mongo
    implementation("it.justwrote:kjob-mongo:$kjobVersion")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}