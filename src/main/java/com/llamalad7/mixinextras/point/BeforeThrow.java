package com.llamalad7.mixinextras.point;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.spongepowered.asm.mixin.injection.IInjectionPointContext;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

import java.util.Collection;
import java.util.ListIterator;

/**
 * @see org.spongepowered.asm.mixin.injection.points.BeforeReturn
 */
@InjectionPoint.AtCode("THROW")
public class BeforeThrow extends InjectionPoint {

    private final int ordinal;

    public BeforeThrow(InjectionPointData data) {
        super(data);

        this.ordinal = data.getOrdinal();
    }

    @Override
    public boolean checkPriority(int targetPriority, int ownerPriority) {
        return true;
    }

    @Override
    public RestrictTargetLevel getTargetRestriction(IInjectionPointContext context) {
        return RestrictTargetLevel.ALLOW_ALL;
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        boolean found = false;

        int throwOpcode = Opcodes.ATHROW;
        int ordinal = 0;

        ListIterator<AbstractInsnNode> iter = insns.iterator();
        while (iter.hasNext()) {
            AbstractInsnNode insn = iter.next();

            if (insn instanceof InsnNode && insn.getOpcode() == throwOpcode) {
                if (this.ordinal == -1 || this.ordinal == ordinal) {
                    nodes.add(insn);
                    found = true;
                }

                ordinal++;
            }
        }

        return found;
    }

    @AtCode("MIXINEXTRAS:THROW")
    public static class BeforeThrowNamespaced extends BeforeThrow {

        public BeforeThrowNamespaced(InjectionPointData data) {
            super(data);
        }
    }
}
