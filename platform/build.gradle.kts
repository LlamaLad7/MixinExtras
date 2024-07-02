subprojects {
    val moduleName = name
    val proguardJar = rootProject.tasks.getByName("proguardJar")

    dependencies {
        compileOnly(mixin())
        compileOnly(asm())
        compileOnly(antlrRuntime())
    }

    tasks.withType<Jar> {
        archiveBaseName.set("mixinextras-$moduleName")
    }

    tasks.named("jar") {
        dependsOn(proguardJar)
    }

    configurePublishing(moduleName) {
        artifact(tasks.getByName("jar"))
        artifact(tasks.getByName("sourcesJar"))
        artifact(tasks.getByName("javadocJar"))
    }
}