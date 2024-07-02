import org.gradle.api.Project
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories

fun Project.mixin(version: String = "0.8"): String {
    repositories {
        maven("https://repo.spongepowered.org/maven")
    }

    return "org.spongepowered:mixin:$version"
}

fun Project.asm(artifact: String = "debug-all", version: String = "5.2"): String {
    repositories {
        mavenCentral()
    }

    return "org.ow2.asm:asm-$artifact:$version"
}