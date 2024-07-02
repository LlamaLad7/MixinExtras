package com.llamalad7.mixinextras.expression.impl;

import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;

public abstract class MEExpressionService {
    private static MEExpressionService instance;

    public static MEExpressionService getInstance() {
        if (instance == null) {
            throw new UnsupportedOperationException("No service has been registered!");
        }
        return instance;
    }

    public static void offerInstance(MEExpressionService candidate) {
        if (instance != null) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot set service instance to %s because it is already set to %s!",
                            candidate, instance
                    )
            );
        }
        instance = candidate;
    }

    public RuntimeException makeInvalidInjectionException(InjectionInfo info, String message) {
        throw runtimeOnly();
    }

    public void decorateInjectorSpecific(InjectionNode node, InjectionInfo info, String key, Object value) {
        throw runtimeOnly();
    }

    private static RuntimeException runtimeOnly() {
        return new UnsupportedOperationException("This operation is only supported at runtime!");
    }
}
