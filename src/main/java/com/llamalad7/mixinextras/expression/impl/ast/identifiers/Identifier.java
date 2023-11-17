package com.llamalad7.mixinextras.expression.impl.ast.identifiers;

import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.io.Serializable;

public interface Identifier extends Serializable {
    boolean matches(IdentifierPool pool, AbstractInsnNode insn);
}
