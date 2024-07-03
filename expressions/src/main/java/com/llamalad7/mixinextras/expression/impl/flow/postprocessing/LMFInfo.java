package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import org.objectweb.asm.Handle;

public class LMFInfo {
    public final Handle impl;
    public final Type type;

    public LMFInfo(Handle impl, Type type) {
        this.impl = impl;
        this.type = type;
    }

    public enum Type {
        FREE_METHOD,
        BOUND_METHOD,
        INSTANTIATION,
    }
}
