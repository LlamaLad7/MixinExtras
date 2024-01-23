package com.llamalad7.mixinextras.expression.impl.pool;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

class ArrayLengthDef implements MemberDefinition {
    @Override
    public boolean matches(AbstractInsnNode insn) {
        return insn.getOpcode() == Opcodes.ARRAYLENGTH;
    }
}
