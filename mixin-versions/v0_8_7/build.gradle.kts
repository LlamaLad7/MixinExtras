plugins {
    id("java-library")
}

dependencies {
    compileOnly(mixin("0.8.7"))
    api(parent!!.project("v0_8_6"))
}