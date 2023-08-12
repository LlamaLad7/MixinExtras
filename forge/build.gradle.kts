repositories {
    maven("https://maven.minecraftforge.net/")
}

dependencies {
    compileOnly(rootProject)
    compileOnly("net.minecraftforge:forge:1.16.4-35.1.37:universal")
}

val proguardFile: File by rootProject.extra

tasks.named<Jar>("jar") {
    from(proguardFile) {
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

tasks.withType<ProcessResources> {
    inputs.property("version", version)

    filesMatching(listOf("META-INF/mods.toml", "META-INF/jarjar/metadata.json")) {
        expand("version" to version)
    }
}