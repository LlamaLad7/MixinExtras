repositories {
    maven("https://maven.minecraftforge.net/")
}

dependencies {
    compileOnly(rootProject)
    for (artifact in arrayOf("forge", "fmlloader", "fmlcore", "javafmllanguage")) {
        compileOnly("net.minecraftforge:$artifact:1.18.2-40.2.4")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

private val proguardFile: File by rootProject.extra

tasks.named<Jar>("jar") {
    setupForgeJar(proguardFile)
}

private val shrunkProguardFile: File by rootProject.extra
private val shrunkProguardJar = rootProject.tasks.getByName("shrunkJar")

val slimJar by tasks.creating(Jar::class) {
    archiveClassifier = "slim"
    from(sourceSets.main.get().output)
    setupForgeJar(shrunkProguardFile)
    dependsOn(shrunkProguardJar)
}

tasks.build {
    dependsOn(slimJar)
}

tasks.withType<ProcessResources> {
    inputs.property("version", version)

    filesMatching(listOf("META-INF/mods.toml", "META-INF/jarjar/metadata.json")) {
        expand("version" to version)
    }
}

fun Jar.setupForgeJar(rootFile: File) {
    from(rootFile) {
        rename { "META-INF/jars/MixinExtras-${project.version}.jar" }
    }
    from(rootProject.file("LICENSE")) {
        rename { "${it}_MixinExtras" }
    }
    manifest.attributes(
        "MixinConfigs" to "mixinextras.init.mixins.json",
        "FMLModType" to "GAMELIBRARY",
    )
}