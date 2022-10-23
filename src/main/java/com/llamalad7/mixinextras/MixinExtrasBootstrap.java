package com.llamalad7.mixinextras;

import com.llamalad7.mixinextras.injector.*;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperationInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;

public class MixinExtrasBootstrap {
    private static boolean initialized = false;
    private static final String VERSION = "0.1.1-rc.1";

    public static void init() {
        if (!initialized) {
            initialized = true;

            InjectionInfo.register(ModifyExpressionValueInjectionInfo.class);
            InjectionInfo.register(ModifyReceiverInjectionInfo.class);
            InjectionInfo.register(ModifyReturnValueInjectionInfo.class);
            InjectionInfo.register(WrapOperationInjectionInfo.class);
            InjectionInfo.register(WrapWithConditionInjectionInfo.class);
        }
    }

    public static String getVersion() {
        return VERSION;
    }
}
