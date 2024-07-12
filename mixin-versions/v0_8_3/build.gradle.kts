plugins {
    id("java-library")
}

dependencies {
    compileOnly(mixin("0.8.3"))
    api(parent!!.project("v0_8"))
}