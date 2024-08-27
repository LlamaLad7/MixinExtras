package com.llamalad7.mixinextras.expression.impl.flow.utils;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.expansion.InsnExpander;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;

/**
 * Keeps track of a particular instruction throughout expansion.
 */
public class InsnReference {
    private AbstractInsnNode insn;

    public InsnReference(FlowValue flow) {
        if (flow.isComplex()) {
            throw new IllegalArgumentException("Cannot create a reference to a complex flow");
        }
        if (InsnExpander.hasExpansion(flow)) {
            InsnExpander.addExpansionStep(flow, injectionNode -> this.insn = injectionNode.getCurrentTarget());
        } else {
            this.insn = flow.getInsn();
        }
    }

    public InjectionNode getNode(Target target) {
        if (insn == null) {
            throw new UnsupportedOperationException("This flow value has not yet been expanded!");
        }
        InjectionNode node = target.getInjectionNode(insn);
        if (node != null) {
            return node;
        }
        if (!target.insns.contains(insn)) {
            throw new IllegalArgumentException("This insn is not present in " + target);
        }
        return target.addInjectionNode(insn);
    }
}
