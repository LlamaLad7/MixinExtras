package com.llamalad7.mixinextras.utils;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

/**
 * Mumfrey, look away.
 */
public class MixinInternals {
    private static final Field TARGET_CLASS_CONTEXT_MIXINS_FIELD;
    private static final Method MIXIN_INFO_GET_STATE_METHOD;
    private static final Field STATE_CLASS_NODE_FIELD;
    private static final Field MEMBER_CURRENT_DESC_FIELD;

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
            Class<?> Member = Class.forName("org.spongepowered.asm.mixin.transformer.ClassInfo$Member");
            MEMBER_CURRENT_DESC_FIELD = Member.getDeclaredField("currentDesc");
            MEMBER_CURRENT_DESC_FIELD.setAccessible(true);
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

    public static void setCurrentDesc(ClassInfo.Method method, String newDesc) {
        try {
            MEMBER_CURRENT_DESC_FIELD.set(method, newDesc);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to use mixin internals, please report to LlamaLad7!", e);
        }
    }
}
