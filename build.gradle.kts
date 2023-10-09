plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "com.github.cheng.bot"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(kotlin("stdlib"))
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.9")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.3.9")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.3.9")
//    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.0.2.RELEASE")
//    implementation("io.projectreactor.addons:reactor-extra:3.3.0.RELEASE")
//    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.0.2.RELEASE")
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
// https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
// https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-annotations
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")

    implementation("io.lettuce:lettuce-core:6.2.6.RELEASE")
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.1.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}