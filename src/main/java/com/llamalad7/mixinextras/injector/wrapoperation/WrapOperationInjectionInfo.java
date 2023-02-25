package com.llamalad7.mixinextras.injector.wrapoperation;

import com.llamalad7.mixinextras.injector.LateApplyingInjectorInfo;
import com.llamalad7.mixinextras.injector.MixinExtrasInjectionInfo;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.points.BeforeConstant;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo.HandlerPrefix;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;

import java.util.List;

@InjectionInfo.AnnotationType(WrapOperation.class)
@HandlerPrefix("wrapOperation")
public class WrapOperationInjectionInfo extends MixinExtrasInjectionInfo implements LateApplyingInjectorInfo {
    private LateApplyingInjectorInfo injectionInfoToQueue = this;

    public WrapOperationInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation, determineAtKey(mixin, method, annotation));
    }

    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        return new WrapOperationInjector(this);
    }

    @Override
    public void inject() {
        WrapOperationApplicatorExtension.offerInjection(this.mixin.getTarget(), injectionInfoToQueue);
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

    @Override
    protected void parseInjectionPoints(List<AnnotationNode> ats) {
        if (this.atKey.equals("at")) {
            super.parseInjectionPoints(ats);
            return;
        }
        // If we're wrapping a `constant`, we need to parse the injection points ourselves.
        Type returnType = Type.getReturnType(this.method.desc);

        for (AnnotationNode at : ats) {
            this.injectionPoints.add(new BeforeConstant(CompatibilityHelper.getMixin(this), at, returnType.getDescriptor()));
        }
    }

    private static String determineAtKey(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        boolean at = Annotations.getValue(annotation, "at") != null;
        boolean constant = Annotations.getValue(annotation, "constant") != null;
        if (at == constant) {
            throw new IllegalStateException(
                    String.format("@WrapOperation injector %s::%s must specify exactly one of `at` and `constant`, got %s.",
                            mixin.getMixin().getClassName(),
                            method.name,
                            at ? "both" : "neither"
                    )
            );
        } else {
            return at ? "at" : "constant";
        }
    }
}
