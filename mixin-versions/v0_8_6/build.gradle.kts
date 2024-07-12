plugins {
    id("java-library")
}

dependencies {
    compileOnly(mixin("0.8.6"))
    api(parent!!.project("v0_8_4"))
}