package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.injector.LateApplyingInjectorInfo;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import com.llamalad7.mixinextras.utils.MixinInternals;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo.AnnotationType;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo.HandlerPrefix;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;

@AnnotationType(SugarWrapper.class)
@HandlerPrefix("sugarWrapper")
public class SugarWrapperInjectionInfo extends InjectionInfo implements LateApplyingInjectorInfo {
    private final InjectionInfo delegate;
    private final AnnotationNode originalAnnotation;
    private final SugarInjector sugarInjector;
    private final boolean lateApply;

    public SugarWrapperInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
        sugarInjector = new SugarInjector(this, mixin.getMixin(), method);
        method.visibleAnnotations.remove(annotation);
        method.visibleAnnotations.add(originalAnnotation = Annotations.getValue(annotation, "original"));
        sugarInjector.stripSugar();
        delegate = InjectionInfo.parse(mixin, method);
        sugarInjector.setTargets(MixinInternals.getTargets(delegate));
        lateApply = delegate instanceof LateApplyingInjectorInfo;
        if (lateApply) {
            ((LateApplyingInjectorInfo) delegate).wrap(this);
        }
    }

    @Override
    protected void readAnnotation() {
    }

    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        return null;
    }

    @Override
    public boolean isValid() {
        return delegate.isValid();
    }

    @Override
    public void prepare() {
        delegate.prepare();
        method.visibleAnnotations.remove(originalAnnotation);
        sugarInjector.prepareSugar();
    }

    // @Override on 0.8.3+
    @SuppressWarnings("unused")
    public void preInject() {
        CompatibilityHelper.preInject(delegate);
    }

    @Override
    public void inject() {
        delegate.inject();
    }

    @Override
    public void postInject() {
        try {
            delegate.postInject();
        } catch (InvalidInjectionException | InjectionError e) {
            for (SugarApplicationException sugarException : sugarInjector.getExceptions()) {
                e.addSuppressed(sugarException);
            }
            throw e;
        }
        if (!lateApply) {
            sugarInjector.applySugar();
        }
    }

    @Override
    public void addCallbackInvocation(MethodNode handler) {
        delegate.addCallbackInvocation(handler);
    }

    @Override
    public void lateApply() {
        try {
            ((LateApplyingInjectorInfo) delegate).lateApply();
        } catch (InvalidInjectionException | InjectionError e) {
            for (SugarApplicationException sugarException : sugarInjector.getExceptions()) {
                e.addSuppressed(sugarException);
            }
            throw e;
        }
        sugarInjector.applySugar();
    }

    @Override
    public void wrap(LateApplyingInjectorInfo outer) {
        throw new UnsupportedOperationException("Cannot wrap a sugar wrapper!");
    }
}
