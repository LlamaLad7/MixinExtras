plugins {
    id("java")
}

configurations.all {
    resolutionStrategy {
        force("org.spongepowered:mixin:0.8")
    }
}