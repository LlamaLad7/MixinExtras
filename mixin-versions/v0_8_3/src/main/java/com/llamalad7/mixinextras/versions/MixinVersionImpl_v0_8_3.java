package com.llamalad7.mixinextras.versions;

import org.objectweb.asm.tree.AnnotationNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.refmap.IMixinContext;

@SuppressWarnings("unused")
public class MixinVersionImpl_v0_8_3 extends MixinVersionImpl_v0_8 {
    @Override
    public IMixinContext getMixin(InjectionInfo info) {
        return info.getMixin();
    }

    @Override
    public void preInject(InjectionInfo info) {
        info.preInject();
    }

    @Override
    public AnnotationNode getAnnotation(InjectionInfo info) {
        return info.getAnnotationNode();
    }
}
