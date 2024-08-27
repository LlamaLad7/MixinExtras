package com.llamalad7.mixinextras.expression.impl.utils;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.utils.InsnReference;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.struct.Target;

public class ComplexComparisonInfo extends ComparisonInfo {
    private final InsnReference jumpInsn;
    private final int jumpOpcode;

    public ComplexComparisonInfo(int comparison, FlowValue node, Type input, FlowValue jump, boolean jumpOnTrue) {
        super(comparison, node, input, jumpOnTrue);
        this.jumpInsn = new InsnReference(jump);
        this.jumpOpcode = jump.getInsn().getOpcode();
    }

    @Override
    public int copyJump(InsnList insns) {
        insns.add(new InsnNode(comparison));
        return jumpOpcode;
    }

    @Override
    public JumpInsnNode getJumpInsn(Target target) {
        return (JumpInsnNode) jumpInsn.getNode(target).getCurrentTarget();
    }

    @Override
    public void cleanup(Target target) {
        target.replaceNode(getJumpInsn(target), new InsnNode(Opcodes.NOP));
    }
}
