package com.llamalad7.mixinextras.expression.impl.utils;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.struct.Target;

public class ComplexComparisonInfo extends ComparisonInfo {
    private final AbstractInsnNode jumpInsn;

    public ComplexComparisonInfo(int comparison, AbstractInsnNode node, Type input, JumpInsnNode jump, boolean jumpOnTrue) {
        super(comparison, node, input, jumpOnTrue);
        this.jumpInsn = jump;
    }

    @Override
    public int copyJump(InsnList insns) {
        insns.add(new InsnNode(node.getOpcode()));
        return jumpInsn.getOpcode();
    }

    @Override
    public JumpInsnNode getJumpInsn() {
        return (JumpInsnNode) jumpInsn;
    }

    @Override
    public void cleanup(Target target) {
        target.replaceNode(jumpInsn, new InsnNode(Opcodes.NOP));
    }
}
