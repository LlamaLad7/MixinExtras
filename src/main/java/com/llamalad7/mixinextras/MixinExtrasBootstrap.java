package com.llamalad7.mixinextras;

import com.llamalad7.mixinextras.injector.ModifyExpressionValueInjectionInfo;
import com.llamalad7.mixinextras.injector.WrapWithConditionInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;

public class MixinExtrasBootstrap {
    private static boolean initialized = false;

    public static final String VERSION = "0.0.2";

    public static void init() {
        if (!initialized) {
            initialized = true;

            InjectionInfo.register(ModifyExpressionValueInjectionInfo.class);
            InjectionInfo.register(WrapWithConditionInjectionInfo.class);
        }
    }
}
