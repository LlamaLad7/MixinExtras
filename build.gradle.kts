plugins {
    java
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    group = "com.llamalad7"
    version = "0.2.0-beta.4"

    repositories {
        mavenCentral()
        maven("https://repo.spongepowered.org/maven")
    }

    dependencies {
        compileOnly("org.spongepowered:mixin:0.8")
        compileOnly("org.apache.commons:commons-lang3:3.3.2")
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

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                groupId = "com.llamalad7.mixinextras"
                artifactId = moduleName

                from(components["java"])
            }
        }
    }
}

subprojects {
    apply(plugin = "java")

    tasks.named<Jar>("jar") {
        from(rootProject.sourceSets.main.get().output)
    }

    tasks.named<Jar>("sourcesJar") {
        from(rootProject.sourceSets.main.get().allSource)
    }
}

tasks.withType<Jar> {
    from("LICENSE") {
        rename { "${it}_MixinExtras"}
    }
}

val Project.moduleName get () = if (parent == null) "common" else name