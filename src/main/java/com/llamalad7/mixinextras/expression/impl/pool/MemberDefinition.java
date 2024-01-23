package com.llamalad7.mixinextras.expression.impl.pool;

import org.objectweb.asm.tree.AbstractInsnNode;

interface MemberDefinition {
    boolean matches(AbstractInsnNode insn);
}
