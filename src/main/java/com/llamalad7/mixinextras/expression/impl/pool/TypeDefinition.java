package com.llamalad7.mixinextras.expression.impl.pool;

import org.objectweb.asm.Type;

public interface TypeDefinition {
    boolean matches(Type type);
}
