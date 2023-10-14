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
val ktor_version:String by project

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.1")
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
    implementation("org.slf4j:slf4j-api:2.0.9")
    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("ch.qos.logback:logback-classic:1.4.11")
    //https://github.com/coil-kt/coil/blob/main/README-zh.md
//    implementation("io.coil-kt:coil:2.4.0")
    //https://github.com/Him188/yamlkt
    implementation("net.mamoe.yamlkt:yamlkt:0.13.0")
    //request
    //https://ktor.io/docs/getting-started-ktor-client.html#add-dependencies
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")


    implementation("io.lettuce:lettuce-core:6.2.6.RELEASE")
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.1.0")
    implementation("io.ktor:ktor-client-cio-jvm:2.3.5")
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