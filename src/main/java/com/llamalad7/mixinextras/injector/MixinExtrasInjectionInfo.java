package com.llamalad7.mixinextras.injector;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

public abstract class MixinExtrasInjectionInfo extends InjectionInfo {
    protected MixinExtrasInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
    }

    protected MixinExtrasInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation, String atKey) {
        super(mixin, method, annotation, atKey);
    }

    @Override
    public String getSliceId(String id) {
        return id == null ? "" : id;
    }
}
