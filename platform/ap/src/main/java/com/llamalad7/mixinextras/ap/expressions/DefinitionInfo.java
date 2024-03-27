package com.llamalad7.mixinextras.ap.expressions;

import com.llamalad7.mixinextras.utils.APInternals;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import java.util.HashMap;
import java.util.Map;

public abstract class DefinitionInfo {
    private final ProcessingEnvironment processingEnv;
    private final TypeElement mixin;
    private final ExecutableElement handler;
    private final AnnotationHandle injector;
    private final AnnotationHandle at;

    public DefinitionInfo(String atType, ProcessingEnvironment processingEnv, TypeElement mixin, ExecutableElement handler, AnnotationHandle injector, String target, Boolean remap) {
        this.processingEnv = processingEnv;
        this.mixin = mixin;
        this.handler = handler;
        this.injector = injector;
        this.at = AnnotationHandle.of(new SyntheticAt(atType, target, remap));
    }

    public void remap() {
        APInternals.registerInjectionPoint(processingEnv, mixin, handler, injector, at);
    }

    private class SyntheticAt implements AnnotationMirror {
        private final String type;
        private final String target;
        private final Boolean remap;
        private final DeclaredType atType;
        private final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues;

        public SyntheticAt(String type, String target, Boolean remap) {
            this.type = type;
            this.target = target;
            this.remap = remap;
            atType = processingEnv.getTypeUtils().getDeclaredType(
                    processingEnv.getElementUtils().getTypeElement(At.class.getName())
            );
            elementValues = makeElementValues();
        }

        @Override
        public DeclaredType getAnnotationType() {
            return atType;
        }

        @Override
        public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValues() {
            return elementValues;
        }

        private Map<? extends ExecutableElement, ? extends AnnotationValue> makeElementValues() {
            Map<ExecutableElement, AnnotationValue> result = new HashMap<>();
            result.put(getAtMethod("value"), makeStringConstant(type));
            result.put(getAtMethod("target"), makeStringConstant(target));
            if (remap != null) {
                result.put(getAtMethod("remap"), makeBooleanConstant(remap));
            }
            return result;
        }

        private ExecutableElement getAtMethod(String name) {
            for (Element e : atType.asElement().getEnclosedElements()) {
                if (e instanceof ExecutableElement && e.getSimpleName().contentEquals(name)) {
                    return (ExecutableElement) e;
                }
            }
            throw new IllegalStateException(
                    String.format(
                            "Could not find method %s in At! Please inform LlamaLad7!",
                            name
                    )
            );
        }

        private AnnotationValue makeStringConstant(String cst) {
            return new AnnotationValue() {
                @Override
                public Object getValue() {
                    return cst;
                }

                @Override
                public String toString() {
                    return '"' + cst + '"';
                }

                @Override
                public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
                    return v.visitString(cst, p);
                }
            };
        }

        private AnnotationValue makeBooleanConstant(boolean cst) {
            return new AnnotationValue() {
                @Override
                public Object getValue() {
                    return cst;
                }

                @Override
                public String toString() {
                    return Boolean.toString(cst);
                }

                @Override
                public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
                    return v.visitBoolean(cst, p);
                }
            };
        }
    }

    public static class Method extends DefinitionInfo {
        public Method(ProcessingEnvironment processingEnv, TypeElement mixin, ExecutableElement handler, AnnotationHandle injector, String target, Boolean remap) {
            super("INVOKE", processingEnv, mixin, handler, injector, target, remap);
        }
    }

    public static class Field extends DefinitionInfo {
        public Field(ProcessingEnvironment processingEnv, TypeElement mixin, ExecutableElement handler, AnnotationHandle injector, String target, Boolean remap) {
            super("FIELD", processingEnv, mixin, handler, injector, target, remap);
        }
    }
}
