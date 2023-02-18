package com.llamalad7.mixinextras;

import com.llamalad7.mixinextras.injector.ModifyExpressionValueInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReceiverInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReturnValueInjectionInfo;
import com.llamalad7.mixinextras.injector.WrapWithConditionInjectionInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperationApplicatorExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperationInjectionInfo;
import com.llamalad7.mixinextras.sugar.impl.SugarApplicatorExtension;
import com.llamalad7.mixinextras.sugar.impl.SugarWrapperInjectionInfo;
import com.llamalad7.mixinextras.sugar.passback.impl.PassBackClassGenerator;
import com.llamalad7.mixinextras.utils.MixinInternals;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;

@SuppressWarnings("unused")
public class MixinExtrasBootstrap {
    private static boolean initialized = false;
    private static final String VERSION = "0.2.0-beta.2";

    public static String getVersion() {
        return VERSION;
    }

    public static void init() {
        initialize(true);
    }

    static void initialize(boolean runtime) {
        if (!initialized) {
            initialized = true;

            InjectionInfo.register(ModifyExpressionValueInjectionInfo.class);
            InjectionInfo.register(ModifyReceiverInjectionInfo.class);
            InjectionInfo.register(ModifyReturnValueInjectionInfo.class);
            InjectionInfo.register(WrapOperationInjectionInfo.class);
            InjectionInfo.register(WrapWithConditionInjectionInfo.class);

            InjectionInfo.register(SugarWrapperInjectionInfo.class);

            if (runtime) {
                MixinInternals.registerExtension(new SugarApplicatorExtension());
                MixinInternals.registerExtension(new WrapOperationApplicatorExtension());

                MixinInternals.registerClassGenerator(PassBackClassGenerator::new);
            }
        }
    }
}
