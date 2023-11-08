package com.llamalad7.mixinextras.expression.impl.wrapper;

import com.llamalad7.mixinextras.transformer.MixinTransformer;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.util.Annotations;

import java.util.Collections;
import java.util.List;

public class ExpressionInjectorWrapperTransformer implements MixinTransformer {
    @Override
    public void transform(IMixinInfo mixinInfo, ClassNode mixinNode) {
        for (MethodNode method : mixinNode.methods) {
            AnnotationNode ann = InjectionInfo.getInjectorAnnotation(mixinInfo, method);
            AnnotationNode inner = ann;
            if (inner == null) continue;
            while (Annotations.getValue(inner, "original") != null) {
                inner = Annotations.getValue(inner, "original");
            }
            Object at = Annotations.getValue(inner, "at");
            if (at == null) continue;
            List<AnnotationNode> ats;
            if (at instanceof List) {
                //noinspection unchecked
                ats = (List<AnnotationNode>) at;
            } else {
                ats = Collections.singletonList((AnnotationNode) at);
            }
            for (AnnotationNode point : ats) {
                Object value = Annotations.getValue(point);
                if ("MIXINEXTRAS:EXPRESSION".equals(value)) {
                    wrapInjectorAnnotation(method, ann);
                    break;
                }
            }
        }
    }

    private void wrapInjectorAnnotation(MethodNode method, AnnotationNode ann) {
        AnnotationNode wrapped = new AnnotationNode(Type.getDescriptor(ExpressionInjectorWrapper.class));
        wrapped.visit("original", ann);
        method.visibleAnnotations.remove(ann);
        method.visibleAnnotations.add(wrapped);
    }
}
