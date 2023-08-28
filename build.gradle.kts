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

val isLocal = (properties["isLocal"] as? String).toBoolean()

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    group = "com.llamalad7"
    version = "0.2.0-beta.11"

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
            from(sourceSets.main.get().allSource)
        }

        tasks.named<Javadoc>("javadoc") {
            classpath += rootProject.configurations.compileClasspath.get()
            source(rootProject.sourceSets.main.get().java)
            source(sourceSets.main.get().java)
        }
    }
}

allprojects {
    extensions.configure<PublishingExtension> {
        if (isLocal) {
            repositories {
                maven {
                    name = "Sonatype"
                    url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                    authentication {
                        credentials {
                            username = properties["ossrhUsername"] as String
                            password = properties["ossrhPassword"] as String
                        }
                    }
                }
            }
        }
        publications {
            create<MavenPublication>("maven") {
                groupId = "io.github.llamalad7"
                artifactId = "mixinextras-$moduleName"

                if (parent == null) {
                    artifact(proguardFile) {
                        builtBy(proguardJar)
                    }
                } else {
                    artifact(tasks.getByName("jar"))
                }
                artifact(tasks.getByName("sourcesJar"))
                artifact(tasks.getByName("javadocJar"))

                pom {
                    name = "MixinExtras"
                    description = "Companion library to Mixin with lots of features to improve the compatibility and concision of your mixins!"
                    url = "https://github.com/LlamaLad7/MixinExtras"
                    licenses {
                        license {
                            name = "MIT License"
                            url = "http://www.opensource.org/licenses/mit-license.php"
                        }
                    }
                    developers {
                        developer {
                            name = "LlamaLad7"
                            url = "https://github.com/LlamaLad7"
                        }
                    }
                    scm {
                        connection = "scm:git:git://github.com/LlamaLad7/MixinExtras.git"
                        developerConnection = "scm:git:git://github.com/LlamaLad7/MixinExtras.git"
                        url = "https://github.com/LlamaLad7/MixinExtras/tree/master"
                    }
                }
            }
        }
    }
    if (isLocal) {
        extensions.configure<SigningExtension> {
            sign(extensions.getByType<PublishingExtension>().publications["maven"])
        }
    }
}

val Project.moduleName get () = if (parent == null) "common" else name