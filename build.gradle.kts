import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import proguard.gradle.ProGuardTask

buildscript {
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.1.0")
    }
}

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

allprojects {
    apply(plugin = "java")

    group = "com.llamalad7"
    version = "0.2.0-rc.3"

    repositories {
        mavenCentral()
        maven("https://repo.spongepowered.org/maven")
    }

    dependencies {
        compileOnly("org.spongepowered:mixin:0.8")
        compileOnly("org.ow2.asm:asm-debug-all:5.2")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8

        withSourcesJar()
        withJavadocJar()
    }
}

val shade by configurations.creating {
    configurations.compileOnly.get().extendsFrom(this)
}

val shadeOnly by configurations.creating

dependencies {
    shade("org.apache.commons:commons-lang3:3.3.2")
    shadeOnly(project("mixin-versions"))
}

tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shade, shadeOnly)
    archiveClassifier = "fat"
    relocate("org.apache.commons.lang3", "com.llamalad7.mixinextras.lib.apache.commons")
    exclude("META-INF/maven/**/*", "META-INF/*.txt")
    from("LICENSE") {
        rename { "${it}_MixinExtras"}
    }
}

val proguardFile by extra { file("build/libs/mixinextras-$version.jar") }

val proguardJar = tasks.create<ProGuardTask>("proguardJar") {
    inputs.files(tasks.shadowJar)
    outputs.files(proguardFile)

    doFirst {
        configurations.compileClasspath.get().resolve().forEach {
            libraryjars(it)
        }
    }

    libraryjars(
        if (JavaVersion.current().isJava9Compatible) {
            "${System.getProperty("java.home")}/jmods"
        } else {
            "${System.getProperty("java.home")}/lib/rt.jar"
        }
    )

    injars(tasks.shadowJar.get().archiveFile)
    outjars(proguardFile)
    configuration(file("proguard.conf"))
}

proguardJar.dependsOn(tasks.shadowJar)
tasks.build.get().dependsOn(proguardJar)

tasks.named<Jar>("jar") {
    archiveClassifier = "slim"
}