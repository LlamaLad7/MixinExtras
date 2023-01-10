package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.sugar.Local;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

abstract class SugarApplicator {
    private static final Map<String, Class<? extends SugarApplicator>> MAP = new HashMap<>();

    static {
        MAP.put(Type.getDescriptor(Local.class), LocalSugarApplicator.class);
    }

    protected final InjectionInfo info;
    protected final Type paramType;
    protected final AnnotationNode sugar;

    SugarApplicator(InjectionInfo info, Type paramType, AnnotationNode sugar) {
        this.info = info;
        this.paramType = paramType;
        this.sugar = sugar;
    }

    abstract void validate(Target target, InjectionNode node);

    abstract void preInject(Target target, InjectionNode node);

    abstract void inject(Target target, InjectionNode node);

    static SugarApplicator create(InjectionInfo info, Type paramType, AnnotationNode sugar) {
        try {
            Class<? extends SugarApplicator> clazz = MAP.get(sugar.desc);
            Constructor<? extends SugarApplicator> ctor = clazz.getDeclaredConstructor(InjectionInfo.class, Type.class, AnnotationNode.class);
            return ctor.newInstance(info, paramType, sugar);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
