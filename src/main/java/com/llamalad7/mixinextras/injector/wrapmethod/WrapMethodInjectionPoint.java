package com.llamalad7.mixinextras.injector.wrapmethod;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.spongepowered.asm.mixin.injection.InjectionPoint;

import java.util.Collection;

class WrapMethodInjectionPoint extends InjectionPoint {
    @Override
    public boolean checkPriority(int targetPriority, int ownerPriority) {
        return true;
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        if (insns.size() == 0) {
            throw new UnsupportedOperationException("Cannot use @WrapMethod on an abstract method!");
        }
        return nodes.add(insns.getFirst());
    }
}
