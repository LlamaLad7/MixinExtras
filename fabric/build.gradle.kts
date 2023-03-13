tasks.withType<ProcessResources> {
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}