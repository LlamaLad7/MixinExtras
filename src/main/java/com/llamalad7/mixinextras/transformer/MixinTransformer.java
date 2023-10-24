package com.llamalad7.mixinextras.transformer;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public interface MixinTransformer {
    void transform(IMixinInfo mixinInfo, ClassNode mixinNode);
}
