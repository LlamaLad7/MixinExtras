package com.llamalad7.mixinextras.expression.impl.pool;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

class TypePoolEntry implements PoolEntry {
    private final Type type;

    TypePoolEntry(Type type) {
        this.type = type;
    }

    @Override
    public boolean matches(AbstractInsnNode insn) {
        if (type.getSort() != Type.ARRAY && type.getSort() != Type.OBJECT) {
            return false;
        }
        return insn instanceof TypeInsnNode && ((TypeInsnNode) insn).desc.equals(type.getInternalName());
    }
}
