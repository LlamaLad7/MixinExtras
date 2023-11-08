package com.llamalad7.mixinextras.expression.impl.ast.identifiers;

import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.objectweb.asm.tree.AbstractInsnNode;

public class PoolIdentifier implements Identifier {
    public final String name;

    public PoolIdentifier(String name) {
        this.name = name;
    }

    @Override
    public boolean matches(IdentifierPool pool, AbstractInsnNode insn) {
        return pool.matches(name, insn);
    }
}
