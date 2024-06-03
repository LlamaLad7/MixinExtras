package com.llamalad7.mixinextras.sugar.impl;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.util.*;

public class SugarPostProcessingExtension implements IExtension {
    private static final Map<String, List<Task>> POST_PROCESSING_TASKS = new HashMap<>();

    static void enqueuePostProcessing(SugarApplicator applicator, Runnable task) {
        POST_PROCESSING_TASKS.computeIfAbsent(applicator.info.getClassNode().name, k -> new ArrayList<>())
                .add(new Task(applicator.postProcessingPriority(), task));
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
        List<Task> tasks = POST_PROCESSING_TASKS.remove(targetName);
        if (tasks != null) {
            Collections.sort(tasks);
            tasks.forEach(Task::run);
        }
    }

    @Override
    public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {
    }

    private static class Task implements Comparable<Task> {
        private final int priority;
        private final Runnable body;

        public Task(int priority, Runnable body) {
            this.priority = priority;
            this.body = body;
        }

        public void run() {
            body.run();
        }

        @Override
        public int compareTo(Task o) {
            return Integer.compare(priority, o.priority);
        }
    }
}
