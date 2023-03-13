package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
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

    protected final IMixinInfo mixin;
    protected final InjectionInfo info;
    protected final AnnotationNode sugar;
    protected final Type paramType;
    protected final Type paramGeneric;
    protected final int paramLvtIndex;
    protected final int paramIndex;

    SugarApplicator(InjectionInfo info, SugarParameter parameter) {
        this.mixin = CompatibilityHelper.getMixin(info).getMixin();
        this.info = info;
        this.sugar = parameter.sugar;
        this.paramType = parameter.type;
        this.paramGeneric = parameter.genericType;
        this.paramLvtIndex = parameter.lvtIndex;
        this.paramIndex = parameter.paramIndex;
    }

    abstract void validate(Target target, InjectionNode node);

    abstract void prepare(Target target, InjectionNode node);

    abstract void inject(Target target, InjectionNode node);

    static SugarApplicator create(InjectionInfo info, SugarParameter parameter) {
        try {
            Class<? extends SugarApplicator> clazz = MAP.get(parameter.sugar.desc);
            Constructor<? extends SugarApplicator> ctor = clazz.getDeclaredConstructor(InjectionInfo.class, SugarParameter.class);
            return ctor.newInstance(info, parameter);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
