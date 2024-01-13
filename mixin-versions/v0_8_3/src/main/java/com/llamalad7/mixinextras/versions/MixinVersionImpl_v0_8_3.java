package com.llamalad7.mixinextras.versions;

import com.llamalad7.mixinextras.utils.MixinInternals_v0_8_3;
import org.objectweb.asm.tree.AnnotationNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

import java.util.Collection;
import java.util.stream.Collectors;

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

    @Override
    public Collection<Target> getTargets(InjectionInfo info) {
        MixinTargetContext mixin = (MixinTargetContext) MixinVersion.getInstance().getMixin(info);
        return MixinInternals_v0_8_3.getTargets(info).stream().map(mixin::getTargetMethod).collect(Collectors.toList());
    }
}
