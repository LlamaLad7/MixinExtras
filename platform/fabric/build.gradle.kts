plugins {
    id("mixinextras.platform")
    id("mixinextras.slimjar")
}

tasks.withType<ProcessResources> {
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

tasks.named<Jar>("jar") {
    manifest.attributes(
        "Fabric-Loom-Remap" to "false",
    )
}