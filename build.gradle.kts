import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively

plugins {
    kotlin("jvm") version "1.9.10"
    application
    `java-library`
    id("org.bytedeco.gradle-javacpp-platform").version("1.5.9")
}

group = "com.github.cheng.bot"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}
val ktor_version: String by project
val retrofit_version: String by project

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.1")
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
    //implementation("io.coil-kt:coil:2.4.0")
    //https://github.com/bytedeco/javacv
    //coil is android library, but running on the server
//    implementation("org.bytedeco:javacv-platform:1.5.9")

    //https://github.com/bytedeco/javacv/issues/2087 upgrade opencv 480
    implementation("org.bytedeco:javacv-platform:1.5.10-SNAPSHOT")

//    implementation("com.groupdocs:groupdocs-conversion:23.10")
    //https://github.com/Him188/yamlkt
    implementation("net.mamoe.yamlkt:yamlkt:0.13.0")

    implementation("io.lettuce:lettuce-core:6.2.6.RELEASE")
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    compileOnly("com.squareup.retrofit2:retrofit:$retrofit_version")
    compileOnly("com.squareup.retrofit2:converter-gson:$retrofit_version")
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

tasks {
    withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE // allow duplicates
        // Otherwise you'll get a "No main manifest attribute" error
        manifest {
            attributes["Main-Class"] = "MainKt"
        }

        // To add all of the dependencies otherwise a "NoClassDefFoundError" error
        from(sourceSets.main.get().output)

        dependsOn(configurations.runtimeClasspath)
        from({
            configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
        })
    }
    named("build") {
        dependsOn("updateI18n")
    }
    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
            freeCompilerArgs += "-Xcontext-receivers"
//            jvmTarget = "8"
        }
        configureEach{
            compilerOptions
                .languageVersion.set(KotlinVersion.KOTLIN_2_0)
        }

    }
}

@OptIn(ExperimentalPathApi::class)
task("updateI18n") {
    //删除resources下的i8n.zip
    //压缩i18n文件夹下的内容为一个压缩包 并输出到它的父路径也就是resource下。
    Files.deleteIfExists(Path("./src/main/resources/i18n.zip"))
    Path("./temp").deleteRecursively()
    Files.deleteIfExists(Path("./temp"))
    val newZipEntryPath = Path("./src/main/resources/i18n.zip")
    val dir = Path("./src/main/resources/i18n/").toFile()
    ZipOutputStream(FileOutputStream(newZipEntryPath.toFile())).use { zipOutPut ->
        dir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".json")) {
                file.inputStream().use { input ->
                    zipOutPut.putNextEntry(ZipEntry(file.name))
                    input.copyTo(zipOutPut)
                    zipOutPut.closeEntry()
                }
            }
        }
        zipOutPut.finish()
    }
}