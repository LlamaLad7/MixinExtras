package com.llamalad7.mixinextras.injector;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.struct.Target;

public class StackExtension {
    private final MethodNode target;

    public StackExtension(Target target) {
        this.target = target.method;
    }

    public void receiver(boolean isStatic) {
        if (!isStatic) {
            target.maxStack++;
        }
    }

    public void capturedArgs(Type[] argTypes, int argCount) {
        for (int i = 0; i < argCount; i++) {
            target.maxStack += argTypes[i].getSize();
        }
    }

    public void extra(int size) {
        target.maxStack += size;
    }

    public void ensureAtLeast(int size) {
        if (target.maxStack < size) {
            target.maxStack = size;
        }
    }
}
