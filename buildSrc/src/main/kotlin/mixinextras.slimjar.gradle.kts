plugins {
    java
}

private val shrunkProguardFile: File by rootProject.extra
private val shrunkProguardJar = rootProject.tasks.getByName("shrunkJar")

val slimJar by tasks.creating(Jar::class) {
    archiveClassifier = "slim"
    from(zipTree(shrunkProguardFile))
    from(sourceSets.main.get().output)
    dependsOn(shrunkProguardJar)

    doLast {
        decompressJar(outputs.files.singleFile)
    }
}

tasks.build {
    dependsOn(slimJar)
}