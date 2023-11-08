package com.llamalad7.mixinextras.expression.impl.ast.identifiers;

import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.objectweb.asm.tree.AbstractInsnNode;

public class WildcardIdentifier implements Identifier {
    @Override
    public boolean matches(IdentifierPool pool, AbstractInsnNode insn) {
        return true;
    }
}