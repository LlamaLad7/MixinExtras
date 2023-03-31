package com.llamalad7.mixinextras.utils;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.util.Counter;

import java.util.HashMap;
import java.util.Map;

public class UniquenessHelper {
    private static final Map<String, Counter> TARGET_TO_COUNTER = Blackboard.getOrPut("UniquenessHelper_TargetToCounter", HashMap::new);

    public static int getNextId(String classRef) {
        return TARGET_TO_COUNTER.computeIfAbsent(classRef, k -> new Counter()).value++;
    }

    public static void clear(String classRef) {
        TARGET_TO_COUNTER.remove(classRef);
    }

    public static class Extension implements IExtension {
        @Override
        public boolean checkActive(MixinEnvironment environment) {
            return true;
        }

        @Override
        public void preApply(ITargetClassContext context) {
        }

        @Override
        public void postApply(ITargetClassContext context) {
            UniquenessHelper.clear(context.getClassNode().name);
        }

        @Override
        public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {
        }
    }
}
