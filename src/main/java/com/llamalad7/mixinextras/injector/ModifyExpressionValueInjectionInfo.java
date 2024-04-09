package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.utils.InjectorUtils;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo.HandlerPrefix;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

@InjectionInfo.AnnotationType(ModifyExpressionValue.class)
@HandlerPrefix("modifyExpressionValue")
public class ModifyExpressionValueInjectionInfo extends MixinExtrasLateInjectionInfo {
    public ModifyExpressionValueInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
    }

    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        return new ModifyExpressionValueInjector(this);
    }

    @Override
    public void prepare() {
        super.prepare();
        InjectorUtils.checkForDupedNews(this.targetNodes);
    }

    @Override
    public String getLateInjectionType() {
        return "ModifyExpressionValue";
    }
}
