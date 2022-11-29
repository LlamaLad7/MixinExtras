# MixinExtras

A small companion library to [Mixin](https://github.com/SpongePowered/Mixin/), designed to help you write your Mixins in
a more expressive and compatible way.

More information about each feature offered can be found at the [Wiki](https://github.com/LlamaLad7/MixinExtras/wiki).

## Setup

For the time being, I recommend depending on this library via [JitPack](https://jitpack.io/). After adding its
repository to your build script, you can depend on it like so

```gradle
dependencies {
    implementation("com.github.LlamaLad7:MixinExtras:0.1.1")
    annotationProcessor("com.github.LlamaLad7:MixinExtras:0.1.1")
}
```

It is essential that you register MixinExtras as an annotation processor if you want remapping of custom features to
work. If you are registering Mixin as an annotation processor too, ***you must register the MixinExtras annotation
processor before the Mixin one.***

### Bundling MixinExtras

If you intend to use this library in your mod, you will probably need to bundle it in your jar.

If you are on Fabric, I recommend using the `include` configuration in your `dependencies` block.

If you are on another platform, I recommend the [Shadow Gradle Plugin](https://imperceptiblethoughts.com/shadow/). I
also highly recommend you [relocate](https://imperceptiblethoughts.com/shadow/configuration/relocation/) MixinExtras to
avoid conflicts with different versions of the library.

## Initialization

The final step after setting up your build script is to initialize MixinExtras. To do this, simply call

```java
MixinExtrasBootstrap.init();
```

somewhere suitably early in your program. On Fabric, this would be a `PreLaunch` entrypoint. If you cannot find a
suitable place for your platform, you might like to use
an [IMixinConfigPlugin](https://github.com/SpongePowered/Mixin/blob/master/src/main/java/org/spongepowered/asm/mixin/extensibility/IMixinConfigPlugin.java)
. Initializing MixinExtras in your plugin's `onLoad` method should work fine on any platform (***except Fabric loader <= 0.14.10!*** [1]).

### You're good to go!

Enjoy using the library, and don't hesitate to open an [issue](https://github.com/LlamaLad7/MixinExtras/issues) if you
have any feedback, questions or suggestions.

## Footnotes
[1] FabricLoader had an issue meaning a crash occurred if the very first class loaded on `KnotClassLoader` was also referenced from a mixin plugin. `MixinExtrasBootstrap` is frequently the first class loaded there since lots of people initialize it from a `preLaunch` entrypoint. As such, please do that and do not initialize it from a mixin plugin. This has been resolved in Fabric Loader 0.14.11, but that's not very widely used yet.
