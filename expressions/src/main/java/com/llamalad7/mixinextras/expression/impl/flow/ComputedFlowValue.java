package com.llamalad7.mixinextras.expression.impl.flow;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.function.Function;

public class ComputedFlowValue extends FlowValue {
    private final int size;
    private final Function<FlowValue[], Type> computer;

    public ComputedFlowValue(int size, Function<FlowValue[], Type> computer, AbstractInsnNode insn, FlowValue... parents) {
        super(null, insn, parents);
        this.size = size;
        this.computer = computer;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public Type getType() {
        return computer.apply(parents);
    }
}
