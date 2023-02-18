package com.llamalad7.mixinextras.utils;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Used only to pass the map between relocated versions. There's probably a better way?
 */
class BlackboardMarkerExtension implements IExtension, Supplier<Map<String, Object>> {
    private final Map<String, Object> map = new HashMap<>();

    @Override
    public Map<String, Object> get() {
        return map;
    }

    @Override
    public boolean checkActive(MixinEnvironment environment) {
        return false;
    }

    @Override
    public void preApply(ITargetClassContext context) {
    }

    @Override
    public void postApply(ITargetClassContext context) {
    }

    @Override
    public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> getImpl() {
        IMixinTransformer transformer = (IMixinTransformer) MixinEnvironment.getDefaultEnvironment().getActiveTransformer();
        Extensions extensions = (Extensions) transformer.getExtensions();
        for (IExtension extension : extensions.getExtensions()) {
            if (extension.getClass().getName().endsWith(".BlackboardMarkerExtension")) {
                return ((Supplier<Map<String, Object>>) extension).get();
            }
        }
        BlackboardMarkerExtension newImpl = new BlackboardMarkerExtension();
        extensions.add(newImpl);
        return newImpl.get();
    }
}
