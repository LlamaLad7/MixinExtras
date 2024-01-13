package com.llamalad7.mixinextras.wrapper;

import com.llamalad7.mixinextras.injector.LateApplyingInjectorInfo;
import com.llamalad7.mixinextras.injector.MixinExtrasInjectionInfo;
import com.llamalad7.mixinextras.utils.MixinInternals;
import com.llamalad7.mixinextras.utils.ProxyUtils;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

import java.util.List;
import java.util.Map;

public abstract class WrapperInjectionInfo extends MixinExtrasInjectionInfo implements LateApplyingInjectorInfo {
    final InjectorWrapperImpl impl;
    private final InjectionInfo delegate;
    private final boolean lateApply;

    protected WrapperInjectionInfo(
            InjectorWrapperImpl.Factory implFactory,
            MixinTargetContext mixin,
            MethodNode method,
            AnnotationNode annotation) {
        super(mixin, method, annotation);
        impl = implFactory.create(this, mixin, method, annotation);
        delegate = impl.getDelegate();
        boolean lateApply = LateApplyingInjectorInfo.wrap(delegate, this);
        if (delegate instanceof WrapperInjectionInfo) {
            WrapperInjectionInfo inner = (WrapperInjectionInfo) delegate;
            lateApply = inner.lateApply;
        } else if (!lateApply && impl.usesGranularInject()) {
            checkDelegate();
        }
        this.lateApply = lateApply;
    }

    @Override
    protected void readAnnotation() {
    }

    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        throw new AssertionError();
    }

    @Override
    public boolean isValid() {
        return impl.isValid();
    }

    @Override
    public void prepare() {
        impl.prepare();
    }

    // @Override on 0.8.3+
    @SuppressWarnings("unused")
    public void preInject() {
        impl.preInject();
    }

    @Override
    public void inject() {
        if (lateApply) {
            delegate.inject();
        } else {
            impl.inject();
        }
    }

    @Override
    public void postInject() {
        if (!lateApply) {
            impl.doPostInject(delegate::postInject);
        }
    }

    @Override
    public void addCallbackInvocation(MethodNode handler) {
        impl.addCallbackInvocation(handler);
    }

    @Override
    public void lateInject() {
        impl.inject();
    }

    @Override
    public void latePostInject() {
        impl.doPostInject(ProxyUtils.getProxy(delegate, LateApplyingInjectorInfo.class)::latePostInject);
    }

    @Override
    public void wrap(LateApplyingInjectorInfo outer) {
        LateApplyingInjectorInfo.wrap(delegate, outer);
    }

    @Override
    public String getLateInjectionType() {
        if (!lateApply) {
            throw new IllegalStateException("Wrapper was asked for its late injection type but does not have one!");
        }
        if (!(delegate instanceof LateApplyingInjectorInfo)) {
            // Must be from an old relocated instance
            return "WrapOperation";
        }
        return ((LateApplyingInjectorInfo) delegate).getLateInjectionType();
    }

    private void checkDelegate() {
        try {
            if (delegate.getClass().getMethod("inject").getDeclaringClass() != InjectionInfo.class) {
                throw impl.granularInjectNotSupported();
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<Target, List<InjectionNode>> getTargetMap() {
        return MixinInternals.getTargets(delegate);
    }
}
