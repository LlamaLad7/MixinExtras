plugins {
    id("mixinextras.platform")
    id("mixinextras.slimjar")
}

repositories {
    maven("https://maven.neoforged.net/releases/")
}

dependencies {
    compileOnly("net.neoforged.fancymodloader:language-java:1.0.2")
    compileOnly("net.neoforged.fancymodloader:loader:1.0.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

jarsNamed("jar", "slimJar") {
    manifest.attributes(
        "MixinConfigs" to "mixinextras.init.mixins.json",
        "FMLModType" to "LIBRARY",
        "Automatic-Module-Name" to "mixinextras.neoforge",
    )
}
