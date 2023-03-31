package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.utils.Blackboard;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SugarPostProcessingExtension implements IExtension {
    private static final Map<String, List<Runnable>> POST_PROCESSING_TASKS =
            Blackboard.getOrPut("Sugar_postProcessingTasks", HashMap::new);

    static void enqueuePostProcessing(SugarApplicator applicator, Runnable task) {
        POST_PROCESSING_TASKS.computeIfAbsent(applicator.info.getClassNode().name, k -> new ArrayList<>()).add(task);
    }

    @Override
    public boolean checkActive(MixinEnvironment environment) {
        return true;
    }

    @Override
    public void preApply(ITargetClassContext context) {
    }

    @Override
    public void postApply(ITargetClassContext context) {
        String targetName = context.getClassNode().name;
        List<Runnable> tasks = POST_PROCESSING_TASKS.get(targetName);
        if (tasks != null) {
            tasks.forEach(Runnable::run);
            POST_PROCESSING_TASKS.remove(targetName);
        }
    }

    @Override
    public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {
    }
}
