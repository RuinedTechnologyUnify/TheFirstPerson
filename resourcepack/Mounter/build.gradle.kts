//import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.9.23"
//    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "kr.kro.lanthanide"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
//    testImplementation(kotlin("test"))
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.test {
    useJUnitPlatform()
}

//tasks {
//    named<ShadowJar>("shadowJar") {
//        archiveBaseName.set("shadow")
//        mergeServiceFiles()
//        minimize()
//        dependencies {
//            exclude(dependency("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT"))
//        }
//        manifest {
//            attributes(mapOf("Main-Class" to "kr.kro.lanthanide.Mounter"))
//        }
//    }
//    build {
//        dependsOn(shadowJar)
//    }
//}

kotlin {
    jvmToolchain(21)
}
tasks.jar {
    manifest.attributes["Main-Class"] = "com.example.MyMainClass"
    val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree) // OR .map { zipTree(it) }
    from(dependencies)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

