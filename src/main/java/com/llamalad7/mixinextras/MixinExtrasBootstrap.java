package com.llamalad7.mixinextras;

import com.llamalad7.mixinextras.injector.ModifyExpressionValueInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReceiverInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReturnValueInjectionInfo;
import com.llamalad7.mixinextras.injector.WrapWithConditionInjectionInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperationInjectionInfo;
import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.service.MixinExtrasVersion;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;

@SuppressWarnings("unused")
public class MixinExtrasBootstrap {
    private static boolean initialized = false;

    public static String getVersion() {
        return MixinExtrasVersion.LATEST.toString();
    }

    public static void init() {
        initialize(true);
    }

    static void initialize(boolean runtime) {
        if (initialized) {
            return;
        }
        initialized = true;

        if (!runtime) {
            InjectionInfo.register(ModifyExpressionValueInjectionInfo.class);
            InjectionInfo.register(ModifyReceiverInjectionInfo.class);
            InjectionInfo.register(ModifyReturnValueInjectionInfo.class);
            InjectionInfo.register(WrapOperationInjectionInfo.class);
            InjectionInfo.register(WrapWithConditionInjectionInfo.class);
            return;
        }

        MixinExtrasService.setup();
    }
}
