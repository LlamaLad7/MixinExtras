package com.llamalad7.mixinextras.expression.impl.ast.identifiers;

import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

public class WildcardIdentifier implements MemberIdentifier, TypeIdentifier {
    @Override
    public boolean matches(IdentifierPool pool, AbstractInsnNode insn) {
        return true;
    }

    @Override
    public boolean matches(IdentifierPool pool, Handle handle) {
        return true;
    }

    @Override
    public boolean matches(IdentifierPool pool, Type type) {
        return true;
    }
}
