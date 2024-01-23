package com.llamalad7.mixinextras.expression.impl.pool;

import org.objectweb.asm.Type;

interface TypeDefinition {
    boolean matches(Type type);
}
