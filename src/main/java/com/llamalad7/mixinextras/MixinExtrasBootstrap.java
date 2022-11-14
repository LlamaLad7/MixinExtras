package com.llamalad7.mixinextras;

import com.llamalad7.mixinextras.injector.*;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperationApplicatorExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperationInjectionInfo;
import com.llamalad7.mixinextras.point.BeforeThrow;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class MixinExtrasBootstrap {
    private static boolean initialized = false;
    private static final String VERSION = "0.1.1";

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
            InjectionInfo.register(InitVariableInjectionInfo.class);
            InjectionInfo.register(RedirectExitInjectionInfo.class);
            InjectionInfo.register(ModifyThrowValueInjectionInfo.class);

            CompatibilityHelper.registerInjectionPoint(BeforeThrow.class, "MixinExtras", BeforeThrow.BeforeThrowNamespaced.class);

            if (runtime) {
                registerExtension(new WrapOperationApplicatorExtension());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void registerExtension(IExtension extension) {
        IMixinTransformer transformer = (IMixinTransformer) MixinEnvironment.getDefaultEnvironment().getActiveTransformer();
        Extensions extensions = (Extensions) transformer.getExtensions();
        extensions.add(extension);
        try {
            // In case we're initialising after the extensions have already been selected, we have to hack ourselves in.
            // If we haven't passed selection yet, it doesn't matter, because the list is re-created then.
            Field activeExtensionsField = Extensions.class.getDeclaredField("activeExtensions");
            activeExtensionsField.setAccessible(true);
            List<IExtension> activeExtensions = new ArrayList<>((List<IExtension>) activeExtensionsField.get(extensions));
            activeExtensions.add(extension);
            activeExtensionsField.set(extensions, Collections.unmodifiableList(activeExtensions));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Fail-fast so people report this and I can fix it
            throw new RuntimeException(
                    String.format("Failed to inject extension %s. Please inform LlamaLad7!", extension),
                    e
            );
        }
    }
}
