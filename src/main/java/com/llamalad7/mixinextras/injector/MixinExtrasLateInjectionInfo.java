package com.llamalad7.mixinextras.injector;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

public abstract class MixinExtrasLateInjectionInfo extends MixinExtrasInjectionInfo implements LateApplyingInjectorInfo {
    private LateApplyingInjectorInfo injectionInfoToQueue = this;

    public MixinExtrasLateInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
    }

    public MixinExtrasLateInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation, String atKey) {
        super(mixin, method, annotation, atKey);
    }

    @Override
    public void inject() {
        LateInjectionApplicatorExtension.offerInjection(this.mixin.getTarget(), injectionInfoToQueue);
    }

    @Override
    public void postInject() {
    }

    @Override
    public void lateInject() {
        super.inject();
    }

    @Override
    public void latePostInject() {
        super.postInject();
    }

    @Override
    public void wrap(LateApplyingInjectorInfo outer) {
        this.injectionInfoToQueue = outer;
    }
}
