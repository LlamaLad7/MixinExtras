package com.llamalad7.mixinextras.injector;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

import java.util.List;

public abstract class MixinExtrasLateInjectionInfo extends MixinExtrasInjectionInfo implements LateApplyingInjectorInfo {
    private LateApplyingInjectorInfo injectionInfoToQueue = this;
    private boolean hasInjectStarted = false;

    public MixinExtrasLateInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
    }

    public MixinExtrasLateInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation, String atKey) {
        super(mixin, method, annotation, atKey);
    }

    @Override
    public void inject() {
        hasInjectStarted = true;
        int callbackTotal = 0;
        for (List<InjectionNode> nodes : targetNodes.values()) {
            callbackTotal += nodes.size();
        }
        for (int i = 0; i < callbackTotal; i++) {
            // Have to add these early since we inject after group validation.
            // May as well add them now, the only way they can fail later is by throwing an exception at which point
            // it's game over anyway.
            super.addCallbackInvocation(method);
        }
        LateInjectionApplicatorExtension.offerInjection(this.mixin.getTarget(), injectionInfoToQueue);
    }

    @Override
    public void postInject() {
    }

    @Override
    public void addCallbackInvocation(MethodNode handler) {
        if (!hasInjectStarted) {
            // We want to allow any additional ones that get added during `prepare` by sugar.
            super.addCallbackInvocation(handler);
        }
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
