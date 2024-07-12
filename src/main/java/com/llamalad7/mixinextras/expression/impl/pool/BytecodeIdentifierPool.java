package com.llamalad7.mixinextras.expression.impl.pool;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Annotations;

public class BytecodeIdentifierPool extends IdentifierPool {
    public BytecodeIdentifierPool(Target target, InjectionInfo info, AnnotationNode poolAnnotation) {
        for (AnnotationNode entry : Annotations.<AnnotationNode>getValue(poolAnnotation, "value", true)) {
            parseEntry(entry, target, info);
        }
    }

    private void parseEntry(AnnotationNode entry, Target target, InjectionInfo info) {
        String id = Annotations.getValue(entry, "id");
        for (String method : Annotations.<String>getValue(entry, "method", true)) {
            addMember(id, new MethodDef(method, info));
        }
        for (String method : Annotations.<String>getValue(entry, "field", true)) {
            addMember(id, new FieldDef(method, info));
        }
        for (Type type : Annotations.<Type>getValue(entry, "type", true)) {
            addType(id, new ExactTypeDef(type));
        }
        for (AnnotationNode local : Annotations.<AnnotationNode>getValue(entry, "local", true)) {
            addMember(id, new LocalDef(local, info, target));
        }
    }
}
