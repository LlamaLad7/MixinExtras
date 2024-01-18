package com.llamalad7.mixinextras.expression.impl.pool;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import org.objectweb.asm.tree.AbstractInsnNode;

interface PoolEntry {
    boolean matches(AbstractInsnNode insn, Identifier.Role role);
}
