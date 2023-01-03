package com.llamalad7.mixinextras.injector.wrapoperation;

import com.llamalad7.mixinextras.sugar.impl.SugarInjector;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.points.BeforeConstant;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo.HandlerPrefix;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@InjectionInfo.AnnotationType(WrapOperation.class)
@HandlerPrefix("wrapOperation")
public class WrapOperationInjectionInfo extends InjectionInfo {
    public WrapOperationInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation, determineAtKey(mixin, method, annotation));
    }

    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        return new WrapOperationInjector(this);
    }

    @Override
    public void inject() {
        WrapOperationApplicatorExtension.QUEUED_INJECTIONS.computeIfAbsent(this.mixin.getTarget(), k -> new ArrayList<>()).add(this);
    }

    @Override
    public void postInject() {
    }

    public void lateApply() {
        super.inject();
        super.postInject();
        for (Target target : this.targetNodes.keySet()) {
            SugarInjector.applyFromInjector(this.classNode, this.method, target.method);
        }
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
