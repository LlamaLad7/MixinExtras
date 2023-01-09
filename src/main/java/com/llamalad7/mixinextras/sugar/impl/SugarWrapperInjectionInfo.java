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
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;

@AnnotationType(SugarWrapper.class)
@HandlerPrefix("sugarWrapper")
public class SugarWrapperInjectionInfo extends InjectionInfo implements LateApplyingInjectorInfo {
    private final InjectionInfo delegate;
    private final AnnotationNode originalAnnotation;

    public SugarWrapperInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
        method.visibleAnnotations.remove(annotation);
        method.visibleAnnotations.add(originalAnnotation = Annotations.getValue(annotation, "original"));
        SugarInjector.stripSugar(method);
        delegate = InjectionInfo.parse(mixin, method);
        if (delegate instanceof LateApplyingInjectorInfo) {
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
        delegate.postInject();
        applySugar();
    }

    @Override
    public void lateApply() {
        ((LateApplyingInjectorInfo) delegate).lateApply();
        applySugar();
    }

    @Override
    public void wrap(LateApplyingInjectorInfo outer) {
        throw new UnsupportedOperationException("Cannot wrap a sugar wrapper!");
    }

    private void applySugar() {
        SugarInjector.applySugar(this, mixin.getMixin(), MixinInternals.getTargets(delegate), method);
    }
}
