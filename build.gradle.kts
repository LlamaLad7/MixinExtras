import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import proguard.gradle.ProGuardTask

buildscript {
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.3.2")
    }
}

plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
    antlr
    id("com.diffplug.spotless") version "6.22.0"
}

allprojects {
    apply(plugin = "java-library")

    group = "com.llamalad7"
    version = "0.5.0-beta.1"

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

    tasks.withType<Javadoc> {
        (options as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    tasks.withType<Jar> {
        dependsOn(":generateGrammarSource")
    }
}

val shade by configurations.creating {
    configurations.compileOnly.get().extendsFrom(this)
}

val shadeOnly by configurations.creating

dependencies {
    antlr("org.antlr:antlr4:4.13.1")
    shadeOnly("org.antlr:antlr4-runtime:4.13.1")
    shade("org.apache.commons:commons-lang3:3.3.2")
    shadeOnly(project("mixin-versions"))
    shade("com.google.code.gson:gson:2.11.0")
    shade("com.github.zafarkhaja:java-semver:0.10.2")
}

tasks.withType<AntlrTask> {
    arguments.addAll(listOf("-package", "com.llamalad7.mixinextras.lib.grammar.expressions"))
}

tasks.withType<JavaCompile> {
    dependsOn("generateGrammarSource")
}

spotless {
    antlr4 {
        target("src/*/antlr/**/*.g4")
        antlr4Formatter()
    }
}

tasks.getByName("generateGrammarSource") {
    dependsOn("spotlessAntlr4Apply")
}

tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shade, shadeOnly)
    archiveClassifier = "fat"
    relocate("org.apache.commons.lang3", "com.llamalad7.mixinextras.lib.apache.commons")
    relocate("org.antlr.v4", "com.llamalad7.mixinextras.lib.antlr")
    relocate("com.google.gson", "com.llamalad7.mixinextras.lib.gson")
    relocate("com.google.errorprone", "com.llamalad7.mixinextras.lib.errorprone")
    relocate("com.github.zafarkhaja.semver", "com.llamalad7.mixinextras.lib.semver")
    exclude("META-INF/maven/**/*", "META-INF/*.txt", "META-INF/proguard/*", "META-INF/LICENSE")
    from("LICENSE") {
        rename { "${it}_MixinExtras"}
    }
}

val proguardFile by extra { file("build/libs/mixinextras-$version.jar") }

val proguardJar = tasks.create<ProGuardTask>("proguardJar") {
    inputs.files(tasks.shadowJar)
    outputs.files(proguardFile)

    doFirst {
        (configurations.compileClasspath.get().resolve() - configurations.antlr.get().resolve()).forEach {
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
