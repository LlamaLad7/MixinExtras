package com.llamalad7.mixinextras.expression.impl.ast.identifiers;

import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;

public class DefinedMemberIdentifier implements MemberIdentifier {
    public final String name;

    public DefinedMemberIdentifier(String name) {
        this.name = name;
    }

    @Override
    public boolean matches(IdentifierPool pool, AbstractInsnNode insn) {
        return pool.matchesMember(name, insn);
    }

    @Override
    public boolean matches(IdentifierPool pool, Handle handle) {
        return pool.matchesMember(name, handle);
    }
}
