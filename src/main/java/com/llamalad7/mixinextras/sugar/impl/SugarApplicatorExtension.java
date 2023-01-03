package com.llamalad7.mixinextras.sugar.impl;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.SortedSet;

public class SugarApplicatorExtension implements IExtension {
    @Override
    public boolean checkActive(MixinEnvironment environment) {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void preApply(ITargetClassContext context) {
        try {
            Field mixinsField = context.getClass().getDeclaredField("mixins");
            mixinsField.setAccessible(true);
            SortedSet<IMixinInfo> mixins = (SortedSet<IMixinInfo>) mixinsField.get(context);
            if (mixins.isEmpty()) return;
            Method getStateMethod = mixins.first().getClass().getDeclaredMethod("getState");
            getStateMethod.setAccessible(true);
            Object firstState = getStateMethod.invoke(mixins.first());
            Field classNodeField = firstState.getClass().getDeclaredField("classNode");
            classNodeField.setAccessible(true);
            for (IMixinInfo mixin : mixins) {
                Object state = getStateMethod.invoke(mixin);
                ClassNode classNode = (ClassNode) classNodeField.get(state);
                SugarInjector.preApply(context.getClassNode().name, classNode, ClassInfo.forName(classNode.name), mixin);
            }
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException("Failed to apply sugar!", e);
        }
    }

    @Override
    public void postApply(ITargetClassContext context) {
        SugarInjector.postApply(context.getClassNode());
    }

    @Override
    public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {
    }
}
