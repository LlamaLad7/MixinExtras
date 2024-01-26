package com.llamalad7.mixinextras.expression.impl.pool;

import org.objectweb.asm.tree.AbstractInsnNode;

public interface MemberDefinition {
    boolean matches(AbstractInsnNode insn);
}
