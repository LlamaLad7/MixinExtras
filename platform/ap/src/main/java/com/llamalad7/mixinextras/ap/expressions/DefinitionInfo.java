package com.llamalad7.mixinextras.ap.expressions;

import com.llamalad7.mixinextras.utils.APInternals;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

public class DefinitionInfo {
    private final TypeElement mixin;
    private final ExecutableElement handler;
    private final AnnotationHandle injector;
    private final AnnotationHandle at;

    public DefinitionInfo(TypeElement mixin, ExecutableElement handler, AnnotationHandle injector, AnnotationHandle at) {
        this.mixin = mixin;
        this.handler = handler;
        this.injector = injector;
        this.at = at;
    }

    public void remap(ProcessingEnvironment processingEnv) {
        APInternals.registerInjectionPoint(processingEnv, mixin, handler, injector, at);
    }
}
