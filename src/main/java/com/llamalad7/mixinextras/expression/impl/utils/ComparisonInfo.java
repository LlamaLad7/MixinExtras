package com.llamalad7.mixinextras.expression.impl.utils;

import org.objectweb.asm.Type;

public class ComparisonInfo {
    private final int opcode;
    private final Type input;
    private final boolean jumpOnTrue;
    private final boolean needsExpanding;

    public ComparisonInfo(int opcode, Type input, boolean jumpOnTrue, boolean needsExpanding) {
        this.opcode = opcode;
        this.input = input;
        this.jumpOnTrue = jumpOnTrue;
        this.needsExpanding = needsExpanding;
    }
}
