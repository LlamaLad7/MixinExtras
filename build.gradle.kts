plugins {
    java
    `maven-publish`
}

group = "com.llamalad7"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven")
}

dependencies {
    implementation("org.spongepowered:mixin:0.8.5")
    implementation("org.apache.commons:commons-lang3:3.3.2")
    implementation("org.ow2.asm:asm-debug-all:5.2")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.llamalad7"
            artifactId = "MixinExtras"
            version = "1.0-SNAPSHOT"

            from(components["java"])
        }
    }
}