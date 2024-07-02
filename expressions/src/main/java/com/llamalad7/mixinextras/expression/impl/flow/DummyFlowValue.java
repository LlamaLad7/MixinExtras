package com.llamalad7.mixinextras.expression.impl.flow;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

public class DummyFlowValue extends FlowValue {
    public static final FlowValue UNINITIALIZED = new DummyFlowValue(Type.VOID_TYPE);

    public DummyFlowValue(Type type) {
        super(type, null, (FlowValue[]) null);
    }

    @Override
    public void addChild(FlowValue value, int index) {
    }

    @Override
    public void finish() {
    }

    @Override
    public AbstractInsnNode getInsn() {
        throw ComplexDataException.INSTANCE;
    }

    @Override
    public FlowValue getInput(int index) {
        throw ComplexDataException.INSTANCE;
    }

    @Override
    public int inputCount() {
        throw ComplexDataException.INSTANCE;
    }

    @Override
    public void mergeInputs(FlowValue[] newInputs, FlowContext ctx) {
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof DummyFlowValue && getType().equals(((DummyFlowValue) obj).getType());
    }

    @Override
    public int hashCode() {
        return getType().hashCode();
    }
}
