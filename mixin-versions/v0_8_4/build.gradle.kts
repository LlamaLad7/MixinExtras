plugins {
    id("java-library")
}

dependencies {
    compileOnly(mixin("0.8.4"))
    api(parent!!.project("v0_8_3"))
}