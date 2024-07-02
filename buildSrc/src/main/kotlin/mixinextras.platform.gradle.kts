plugins {
    java
    `maven-publish`
    signing
}

private val sourceInclusions = listOf(
    project(":mixin-versions").subprojects.toList()
).flatten()

private val proguardFile: File by rootProject.extra

dependencies {
    implementation(rootProject)
}

tasks.named<Jar>("jar") {
    from(zipTree(proguardFile))
}

tasks.named<Jar>("sourcesJar") {
    from(rootProject.sourceSets.main.get().java)
    sourceInclusions.forEach {
        from(it.sourceSets.main.get().java)
    }
}

tasks.named<Javadoc>("javadoc") {
    classpath += rootProject.configurations.compileClasspath.get()
    source(rootProject.sourceSets.main.get().java)
    sourceInclusions.forEach {
        source(it.sourceSets.main.get().java)
    }
}
