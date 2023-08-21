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
    apply(plugin = "maven-publish")

    group = "com.llamalad7"
    version = "0.2.0-beta.10"

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
    }

    tasks.withType<Jar> {
        archiveBaseName.set("mixinextras-$moduleName")
    }
}

val shade by configurations.creating {
    configurations.compileOnly.get().extendsFrom(this)
}

dependencies {
    shade("org.apache.commons:commons-lang3:3.3.2")
}

tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shade)
    archiveClassifier = "fat"
    relocate("org.apache.commons.lang3", "com.llamalad7.mixinextras.lib.apache.commons")
    exclude("META-INF/maven/**/*", "META-INF/*.txt")
    from("LICENSE") {
        rename { "${it}_MixinExtras"}
    }
}

val proguardFile by extra { file("build/libs/mixinextras-common-$version.jar") }

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

subprojects {
    apply(plugin = "java")

    tasks.named<Jar>("jar") {
        dependsOn(proguardJar)
    }

    if (project.name != "forge") {
        dependencies {
            compileOnly(rootProject)
        }

        tasks.named<Jar>("jar") {
            from(zipTree(proguardFile))
        }

        tasks.named<Jar>("sourcesJar") {
            from(rootProject.sourceSets.main.get().allSource)
        }
    }
}

allprojects {
    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                groupId = "com.llamalad7.mixinextras"
                artifactId = "mixinextras-$moduleName"

                if (parent == null) {
                    artifact(proguardFile) {
                        builtBy(proguardJar)
                    }
                } else {
                    artifact(tasks.getByName("jar"))
                }
                artifact(tasks.getByName("sourcesJar"))
            }
        }
    }
}

val Project.moduleName get () = if (parent == null) "common" else name