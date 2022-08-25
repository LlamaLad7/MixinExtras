package com.llamalad7.mixinextras.injector.wrapoperation;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo.HandlerPrefix;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

@InjectionInfo.AnnotationType(WrapOperation.class)
@HandlerPrefix("wrapOperation")
public class WrapOperationInjectionInfo extends InjectionInfo {
    public WrapOperationInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
    }

    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        return new WrapOperationInjector(this);
    }

    @Override
    public void postInject() {
        ((WrapOperationInjector) injector).performInjections();
        super.postInject();
    }
}
