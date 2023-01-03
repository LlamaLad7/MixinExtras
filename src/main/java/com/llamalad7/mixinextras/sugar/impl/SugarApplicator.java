package com.llamalad7.mixinextras.sugar.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class SugarApplicator {
    @SuppressWarnings("unchecked")
    private static final Class<? extends SugarApplicator>[] APPLICATORS = new Class[]{LocalSugarApplicator.class};
    private static final Map<String, SugarApplicator> MAP = new HashMap<>();

    protected final String annotationDesc;

    SugarApplicator(Class<? extends Annotation> annotation) {
        this.annotationDesc = Type.getDescriptor(annotation);
    }

    abstract void preInject(IMixinInfo mixin, List<Pair<Type, AnnotationNode>> sugarInfos, Target target, InjectionNode node);

    abstract void inject(IMixinInfo mixin, Type paramType, AnnotationNode sugar, Target target, InjectionNode node);

    static void preApply(IMixinInfo mixin, List<Pair<Type, AnnotationNode>> sugarInfos, Target target, InjectionNode node) {
        prepareMap();
        for (SugarApplicator applicator : MAP.values()) {
            applicator.preInject(mixin, sugarInfos, target, node);
        }
    }

    static void apply(IMixinInfo mixin, Type paramType, AnnotationNode sugar, Target target, InjectionNode node) {
        prepareMap();
        MAP.get(sugar.desc).inject(mixin, paramType, sugar, target, node);
    }

    private static void prepareMap() {
        if (MAP.size() == APPLICATORS.length) {
            return;
        }
        for (Class<? extends SugarApplicator> clazz : APPLICATORS) {
            SugarApplicator applicator;
            try {
                applicator = clazz.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            MAP.put(applicator.annotationDesc, applicator);
        }
    }
}
