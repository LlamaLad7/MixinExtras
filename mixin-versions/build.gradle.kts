plugins {
    id("java")
}

dependencies {
    implementation(project("v0_8_4"))
}

subprojects {
    dependencies {
        compileOnly(rootProject)
    }
}