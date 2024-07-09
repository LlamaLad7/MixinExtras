plugins {
    java
    `maven-publish`
    signing
}

private val javadocInclusions = arrayOf(
    project(":expressions"),
)

private val sourceInclusions = arrayOf(
    *javadocInclusions,
    *project(":mixin-versions").subprojects.toTypedArray(),
)

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
    javadocInclusions.forEach {
        source(it.sourceSets.main.get().java)
    }
}
