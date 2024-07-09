plugins {
    id("java")
}

dependencies {
    implementation(project("v0_8_7"))
}

subprojects {
    dependencies {
        compileOnly(asm())
        compileOnly(rootProject)
    }
}