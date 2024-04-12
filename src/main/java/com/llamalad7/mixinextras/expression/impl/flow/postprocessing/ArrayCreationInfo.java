package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import org.objectweb.asm.tree.AbstractInsnNode;

public class ArrayCreationInfo {
    public final AbstractInsnNode initialized;

    public ArrayCreationInfo(AbstractInsnNode initialized) {
        this.initialized = initialized;
    }
}
