package com.llamalad7.mixinextras.expression.impl.point;

import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.transformer.MixinTransformer;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.Decorations;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.util.Annotations;

import java.util.ArrayList;
import java.util.List;

public class ExpressionSliceMarkerTransformer implements MixinTransformer {
    @Override
    public void transform(IMixinInfo mixinInfo, ClassNode mixinNode) {
        for (MethodNode method : mixinNode.methods) {
            if (ASMUtils.getRepeatedMEAnnotation(method, Expression.class) != null) {
                AnnotationNode ann = InjectionInfo.getInjectorAnnotation(mixinInfo, method);
                for (AnnotationNode slice : Annotations.<AnnotationNode>getValue(ann, "slice", true)) {
                    markAt(Annotations.getValue(slice, "from"));
                    markAt(Annotations.getValue(slice, "to"));
                }
            }
        }
    }

    private void markAt(AnnotationNode at) {
        if (at == null || !Annotations.getValue(at).equals("MIXINEXTRAS:EXPRESSION")) {
            return;
        }
        List<String> args = Annotations.getValue(at, "args");
        if (args == null) {
            at.visit("args", args = new ArrayList<>());
        }
        args.add(Decorations.IS_IN_SLICE + "=true");
    }
}
