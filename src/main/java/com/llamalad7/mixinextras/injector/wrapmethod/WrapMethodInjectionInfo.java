package com.llamalad7.mixinextras.injector.wrapmethod;

import com.llamalad7.mixinextras.injector.MixinExtrasInjectionInfo;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo.HandlerPrefix;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

import java.util.List;

@InjectionInfo.AnnotationType(WrapMethod.class)
@HandlerPrefix("wrapMethod")
public class WrapMethodInjectionInfo extends MixinExtrasInjectionInfo {
    public WrapMethodInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
    }

    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        return new WrapMethodInjector(this);
    }

    @Override
    protected void parseInjectionPoints(List<AnnotationNode> ats) {
        this.injectionPoints.add(new WrapMethodInjectionPoint());
    }
}
