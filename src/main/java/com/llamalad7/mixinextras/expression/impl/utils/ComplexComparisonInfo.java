package com.llamalad7.mixinextras.expression.impl.utils;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

public class ComplexComparisonInfo extends ComparisonInfo {
    private final AbstractInsnNode jumpInsn;

    public ComplexComparisonInfo(int opcode, Type input, AbstractInsnNode jumpInsn, boolean jumpOnTrue) {
        super(opcode, input, jumpOnTrue, false);
        this.jumpInsn = jumpInsn;
    }
}
