plugins {
    java
    `maven-publish`
}

group = "com.llamalad7"
version = "0.0.8"

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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.llamalad7"
            artifactId = "MixinExtras"

            from(components["java"])
        }
    }
}