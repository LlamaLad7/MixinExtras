package com.llamalad7.mixinextras.expression.impl.pool;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import org.objectweb.asm.tree.AbstractInsnNode;

class PrimitiveCastPoolEntry implements PoolEntry {
    private final int[] opcodes;

    PrimitiveCastPoolEntry(int... opcodes) {
        this.opcodes = opcodes;
    }

    @Override
    public boolean matches(AbstractInsnNode insn, Identifier.Role role) {
        if (role != Identifier.Role.TYPE) {
            return false;
        }
        for (int opcode : opcodes) {
            if (insn.getOpcode() == opcode) {
                return true;
            }
        }
        return false;
    }
}
