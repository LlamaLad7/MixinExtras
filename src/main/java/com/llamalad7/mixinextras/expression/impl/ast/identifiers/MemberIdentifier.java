package com.llamalad7.mixinextras.expression.impl.ast.identifiers;

import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;

public interface MemberIdentifier {
    boolean matches(IdentifierPool pool, AbstractInsnNode insn);

    boolean matches(IdentifierPool pool, Handle handle);
}
