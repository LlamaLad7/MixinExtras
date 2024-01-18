package com.llamalad7.mixinextras.expression.impl.pool;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

class ThisPoolEntry implements PoolEntry {
    @Override
    public boolean matches(AbstractInsnNode insn, Identifier.Role role) {
        return role == Identifier.Role.MEMBER && insn.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) insn).var == 0;
    }
}
