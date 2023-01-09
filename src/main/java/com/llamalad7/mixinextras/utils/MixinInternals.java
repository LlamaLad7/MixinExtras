package com.llamalad7.mixinextras.utils;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Mumfrey, look away.
 */
public class MixinInternals {
    private static final Field TARGET_CLASS_CONTEXT_MIXINS_FIELD;
    private static final Method MIXIN_INFO_GET_STATE_METHOD;
    private static final Field STATE_CLASS_NODE_FIELD;
    private static final Field EXTENSIONS_FIELD;
    private static final Field ACTIVE_EXTENSIONS_FIELD;
    private static final Field INJECTION_INFO_TARGET_NODES_FIELD;

    static {
        try {
            Class<?> TargetClassContext = Class.forName("org.spongepowered.asm.mixin.transformer.TargetClassContext");
            TARGET_CLASS_CONTEXT_MIXINS_FIELD = TargetClassContext.getDeclaredField("mixins");
            TARGET_CLASS_CONTEXT_MIXINS_FIELD.setAccessible(true);
            Class<?> MixinInfo = Class.forName("org.spongepowered.asm.mixin.transformer.MixinInfo");
            MIXIN_INFO_GET_STATE_METHOD = MixinInfo.getDeclaredMethod("getState");
            MIXIN_INFO_GET_STATE_METHOD.setAccessible(true);
            Class<?> State = Class.forName("org.spongepowered.asm.mixin.transformer.MixinInfo$State");
            STATE_CLASS_NODE_FIELD = State.getDeclaredField("classNode");
            STATE_CLASS_NODE_FIELD.setAccessible(true);
            INJECTION_INFO_TARGET_NODES_FIELD = InjectionInfo.class.getDeclaredField("targetNodes");
            INJECTION_INFO_TARGET_NODES_FIELD.setAccessible(true);
            EXTENSIONS_FIELD = Extensions.class.getDeclaredField("extensions");
            EXTENSIONS_FIELD.setAccessible(true);
            ACTIVE_EXTENSIONS_FIELD = Extensions.class.getDeclaredField("activeExtensions");
            ACTIVE_EXTENSIONS_FIELD.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException e) {
            throw new RuntimeException("Failed to access some mixin internals, please report to LlamaLad7!", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Pair<IMixinInfo, ClassNode>> getMixinsFor(ITargetClassContext context) {
        try {
            List<Pair<IMixinInfo, ClassNode>> result = new ArrayList<>();
            SortedSet<IMixinInfo> mixins = (SortedSet<IMixinInfo>) TARGET_CLASS_CONTEXT_MIXINS_FIELD.get(context);
            for (IMixinInfo mixin : mixins) {
                Object state = MIXIN_INFO_GET_STATE_METHOD.invoke(mixin);
                ClassNode classNode = (ClassNode) STATE_CLASS_NODE_FIELD.get(state);
                result.add(Pair.of(mixin, classNode));
            }
            return result;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to use mixin internals, please report to LlamaLad7!", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Collection<Target> getTargets(InjectionInfo info) {
        try {
            return ((Map<Target, ?>) INJECTION_INFO_TARGET_NODES_FIELD.get(info)).keySet();
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to use mixin internals, please report to LlamaLad7!", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void registerExtension(IExtension extension) {
        try {
            IMixinTransformer transformer = (IMixinTransformer) MixinEnvironment.getDefaultEnvironment().getActiveTransformer();
            Extensions extensions = (Extensions) transformer.getExtensions();
            List<IExtension> extensionsList = (List<IExtension>) EXTENSIONS_FIELD.get(extensions);
            addExtension(extensionsList, extension);
            List<IExtension> activeExtensions = new ArrayList<>((List<IExtension>) ACTIVE_EXTENSIONS_FIELD.get(extensions));
            addExtension(activeExtensions, extension);
            ACTIVE_EXTENSIONS_FIELD.set(extensions, Collections.unmodifiableList(activeExtensions));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to use mixin internals, please report to LlamaLad7!", e);
        }
    }

    /**
     * This keeps the extensions in "groups", because when there are multiple relocated versions active that's the
     * behaviour we want.
     */
    private static void addExtension(List<IExtension> extensions, IExtension newExtension) {
        String extensionClassName = newExtension.getClass().getName();
        extensionClassName = extensionClassName.substring(extensionClassName.lastIndexOf('.'));
        int index = -1;
        for (int i = 0; i < extensions.size(); i++) {
            IExtension extension = extensions.get(i);
            if (extension.getClass().getName().endsWith(extensionClassName)) {
                index = i;
            }
        }
        if (index == -1) {
            extensions.add(newExtension);
        } else {
            extensions.add(index + 1, newExtension);
        }
    }
}
