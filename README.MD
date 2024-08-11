# MixinExtras

[![](https://dcbadge.vercel.app/api/server/VUkAeRYqdH)](https://llama.is-a.dev/discord)

A small companion library to [Mixin](https://github.com/SpongePowered/Mixin/), designed to help you write your Mixins in
a more expressive and compatible way.

More information about each feature offered can be found at the [Wiki](https://github.com/LlamaLad7/MixinExtras/wiki).

## Setup

MixinExtras is available on Maven Central.

The setup steps vary based on your platform:
<details><summary>Fabric / Quilt</summary>

**FabricLoader 0.15.0+ includes MixinExtras already.** If you want to maintain compatibility with older versions, or
want to use a different version than is provided, see below:

```gradle
dependencies {
    include(implementation(annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.4.1")))
}
```

</details>
<details><summary>NeoForge with NeoGradle</summary>

**NeoForge 20.2.84+ includes MixinExtras already.** If you want to maintain compatibility with older versions, or
want to use a different version than is provided, see below:

```gradle
dependencies {
    implementation(jarJar("io.github.llamalad7:mixinextras-neoforge:0.4.1")) {
        jarJar.ranged(it, "[0.4.1,)")
    }
}
```

</details>
<details><summary>Forge 1.18.2+ with ForgeGradle</summary>

```gradle
dependencies {
    compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:0.4.1"))
    implementation(jarJar("io.github.llamalad7:mixinextras-forge:0.4.1")) {
        jarJar.ranged(it, "[0.4.1,)")
    }
}
```

</details>
<details><summary>Forge 1.18.2+ with Architectury Loom</summary>

```gradle
dependencies {
    compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:0.4.1"))
    implementation(include("io.github.llamalad7:mixinextras-forge:0.4.1"))
}
```

</details>
<details><summary>Any other platform</summary>

This is only a rough guide. You will need to look into the specifics of setting up ShadowJar for your platform.

```gradle
plugins {
    id "com.github.johnrengelman.shadow" version "8.1.0"
}

configurations {
    implementation.extendsFrom shadow
}

dependencies {
    shadow(annotationProcessor("io.github.llamalad7:mixinextras-common:0.4.1"))
}

shadowJar {
    configurations = [project.configurations.shadow]
    relocate("com.llamalad7.mixinextras", "your.package.goes.here.mixinextras")
    mergeServiceFiles() // Very important!
}
```

To initialize MixinExtras, simply call

```java
MixinExtrasBootstrap.init();
```

somewhere suitably early. In almost all cases I would recommend making
an [IMixinConfigPlugin](https://github.com/SpongePowered/Mixin/blob/master/src/main/java/org/spongepowered/asm/mixin/extensibility/IMixinConfigPlugin.java)
and initializing MixinExtras in its `onLoad` method.

</details>

### You're good to go!

Enjoy using the library, and don't hesitate to open an [issue](https://github.com/LlamaLad7/MixinExtras/issues) if you
have any feedback, questions or suggestions.

## Acknowledgements

<img src="https://www.yourkit.com/images/yklogo.png"></img>

I use [YourKit's Java profiler](https://www.yourkit.com/java/profiler/) when working on MixinExtras. Many thanks to them
for providing a free license for the project.
