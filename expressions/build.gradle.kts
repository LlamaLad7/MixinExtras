plugins {
    antlr
    id("com.diffplug.spotless") version "6.22.0"
}

dependencies {
    api(mixin())
    api(asm())
    api(apacheCommons())
    api(antlrRuntime())
    antlr("org.antlr:antlr4:4.13.1")
}

tasks.withType<Jar> {
    archiveBaseName.set("mixinextras-$name")
}

tasks.withType<JavaCompile> {
    dependsOn("generateGrammarSource")
}

tasks.withType<AntlrTask> {
    arguments.addAll(listOf("-package", "com.llamalad7.mixinextras.lib.grammar.expressions"))
    outputDirectory = outputDirectory.resolve("com/llamalad7/mixinextras/lib/grammar/expressions")
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

configurePublishing(name) {
    artifact(tasks.getByName("jar"))
    artifact(tasks.getByName("sourcesJar"))
    artifact(tasks.getByName("javadocJar"))
}