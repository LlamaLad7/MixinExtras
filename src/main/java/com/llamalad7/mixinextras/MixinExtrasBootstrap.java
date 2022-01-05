package com.llamalad7.mixinextras;

import com.llamalad7.mixinextras.injector.ModifyExpressionValueInjectionInfo;
import com.llamalad7.mixinextras.injector.WrapWithConditionInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;

public class MixinExtrasBootstrap {
    public static void init() {
        InjectionInfo.register(ModifyExpressionValueInjectionInfo.class);
        InjectionInfo.register(WrapWithConditionInjectionInfo.class);
    }
}
