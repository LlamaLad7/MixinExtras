plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.guardsquare:proguard-gradle:7.7.0")
    implementation("org.jreleaser:jreleaser-gradle-plugin:1.18.0")
}