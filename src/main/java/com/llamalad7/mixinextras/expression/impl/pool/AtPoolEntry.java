package com.llamalad7.mixinextras.expression.impl.pool;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

class AtPoolEntry implements PoolEntry {
    private final Set<AbstractInsnNode> matched = Collections.newSetFromMap(new IdentityHashMap<>());

    AtPoolEntry(AnnotationNode at, InjectionInfo info, Target target) {
        MethodNode method = target.method;
        InjectionPoint.parse(info, at).find(method.desc, method.instructions, matched);
    }

    @Override
    public boolean matches(AbstractInsnNode insn, Identifier.Role role) {
        return role == Identifier.Role.MEMBER && matched.contains(insn);
    }
}
