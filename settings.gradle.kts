plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "MixinExtras"
include("expressions")
include("platform")
include("platform:common")
include("platform:fabric")
include("platform:forge")
include("platform:neoforge")
include("mixin-versions")
include("mixin-versions:v0_8")
include("mixin-versions:v0_8_3")
include("mixin-versions:v0_8_4")
include("mixin-versions:v0_8_6")
include("mixin-versions:v0_8_7")
