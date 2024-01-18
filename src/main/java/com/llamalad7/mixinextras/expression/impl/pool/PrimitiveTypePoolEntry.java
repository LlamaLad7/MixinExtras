package com.llamalad7.mixinextras.expression.impl.pool;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.spongepowered.asm.util.Bytecode;

public class PrimitiveTypePoolEntry implements PoolEntry {
    private final String boxedClass;

    public PrimitiveTypePoolEntry(Type type) {
        this.boxedClass = Bytecode.getBoxingType(type);
    }

    @Override
    public boolean matches(AbstractInsnNode insn, Identifier.Role role) {
        if (role != Identifier.Role.TYPE || insn.getOpcode() != Opcodes.GETSTATIC) {
            return false;
        }
        FieldInsnNode get = (FieldInsnNode) insn;
        return get.name.equals("TYPE") && get.owner.equals(boxedClass) && get.desc.equals(Type.getDescriptor(Class.class));
    }
}
