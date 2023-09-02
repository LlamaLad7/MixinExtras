plugins {
    id("java-library")
}

dependencies {
    api(parent!!.project("v0_8"))
}

configurations.all {
    resolutionStrategy {
        force("org.spongepowered:mixin:0.8.3")
    }
}