plugins {
    antlr
    id("com.diffplug.spotless") version "6.22.0"
}

dependencies {
    antlr("org.antlr:antlr4:4.13.1")
    api("org.antlr:antlr4-runtime:4.13.1")
    api("org.ow2.asm:asm-debug-all:5.2")
    implementation("org.spongepowered:mixin:0.8.3")
}

tasks.withType<AntlrTask> {
    outputDirectory = outputDirectory.resolve("com/llamalad7/mixinextras/ap/grammar")
    arguments.addAll(listOf("-package", "com.llamalad7.mixinextras.ap.grammar"))
}

tasks.withType<JavaCompile> {
    dependsOn("generateGrammarSource")
}

tasks.withType<Jar> {
    dependsOn("generateGrammarSource")
}

spotless {
    antlr4 {
        target("src/*/antlr/**/*.g4")
        antlr4Formatter()
    }
}

tasks.getByName("generateGrammarSource") {
    dependsOn("spotlessAntlr4Apply")
}