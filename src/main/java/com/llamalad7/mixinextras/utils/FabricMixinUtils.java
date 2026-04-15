package com.llamalad7.mixinextras.utils;

import org.spongepowered.asm.mixin.extensibility.IMixinConfig;

public class FabricMixinUtils {
    private static final String KEY_COMPATIBILITY = "fabric-compat";

    public static int getCompatibility(IMixinConfig config) {
        return CompatibilityHelper.getDecoration(config, KEY_COMPATIBILITY, 0);
    }
}
