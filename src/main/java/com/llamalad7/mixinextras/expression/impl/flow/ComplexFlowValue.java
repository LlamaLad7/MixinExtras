package com.llamalad7.mixinextras.expression.impl.flow;

import com.llamalad7.mixinextras.utils.ASMUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.Set;

public class ComplexFlowValue extends FlowValue {
    private final Set<FlowValue> components;

    public ComplexFlowValue(int size, Set<FlowValue> components) {
        super(size, null, (FlowValue[]) null);
        this.components = components;
    }

    @Override
    public void addChild(FlowValue value, int index) {
    }

    @Override
    public void finish() {
    }

    @Override
    protected Type computeType(FlowValue... inputs) {
        Type type = components.stream().map(FlowValue::getType).reduce(ASMUtils::getCommonSupertype).get();
        if (type == Type.VOID_TYPE) {
            throw ComplexDataException.INSTANCE;
        }
        return type;
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
    public Set<FlowValue> getComponents() {
        return components;
    }

    @Override
    public void mergeInputs(FlowValue[] newInputs) {
    }
}
