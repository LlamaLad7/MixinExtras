import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

buildscript {
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.3.2")
    }
}

plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

allprojects {
    apply(plugin = "java-library")

    group = "com.llamalad7"
    version = "0.5.0-beta.3"

    repositories {
        mavenCentral()
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
        dependsOn(":expressions:generateGrammarSource")
    }
}

val library by configurations.creating {
    configurations.compileOnly.get().extendsFrom(this)
}

val shade by configurations.creating {
    configurations.compileOnly.get().extendsFrom(this)
}

val shadeOnly by configurations.creating

dependencies {
    library(mixin())
    library(asm())
    shadeOnly(antlrRuntime())
    shade(apacheCommons())
    shadeOnly(project("mixin-versions"))
    shade("com.google.code.gson:gson:2.11.0")
    shade("com.github.zafarkhaja:java-semver:0.10.2")
    shade(project("expressions").also { it.isTransitive = false })
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
        rename { "${it}_MixinExtras" }
    }
}

val proguardFile by extra { file("build/libs/mixinextras-$version.jar") }
val proguardJar = createProGuardTask(
    "proguardJar",
    tasks.shadowJar.get().archiveFile.get().asFile,
    proguardFile,
    "proguard.conf"
).apply {
    dependsOn(tasks.shadowJar)
    tasks.build.get().dependsOn(this)
}

val shrunkProguardFile by extra { file("build/libs/mixinextras-$version-shrunk.jar") }
val shrunkProguardJar = createProGuardTask(
    "shrunkJar",
    proguardFile,
    shrunkProguardFile,
    "proguard-shrink.conf"
).apply {
    dependsOn(proguardJar)
    tasks.build.get().dependsOn(this)
    doLast {
        decompressJar(shrunkProguardFile)
    }
}

tasks.named<Jar>("jar") {
    archiveClassifier = "slim"
}
