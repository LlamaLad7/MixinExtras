package com.llamalad7.mixinextras.expression.impl.flow;

import org.objectweb.asm.Type;

@FunctionalInterface
public interface TypeSupplier {
    Type get(FlowValue[] inputs);

    default TypeSupplier memoize() {
        TypeSupplier original = this;
        return new TypeSupplier() {
            private Type result;

            @Override
            public Type get(FlowValue[] inputs) {
                if (result == null) {
                    result = original.get(inputs);
                }
                return result;
            }
        };
    }
}
