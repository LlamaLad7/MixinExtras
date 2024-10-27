plugins {
    id("mixinextras.platform")
    id("mixinextras.slimjar")
}

repositories {
    maven("https://maven.neoforged.net/")
}

dependencies {
    compileOnly("net.neoforged.fancymodloader:language-java:1.0.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.named<Jar>("jar") {
    manifest.attributes(
        "MixinConfigs" to "mixinextras.init.mixins.json",
        "FMLModType" to "GAMELIBRARY",
        "Automatic-Module-Name" to "mixinextras.neoforge",
    )
}