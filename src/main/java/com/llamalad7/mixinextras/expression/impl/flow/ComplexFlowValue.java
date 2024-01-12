package com.llamalad7.mixinextras.expression.impl.flow;

import com.llamalad7.mixinextras.utils.TypeUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.HashSet;
import java.util.Set;

public class ComplexFlowValue extends FlowValue {
    private final int size;
    private final Set<FlowValue> sources;

    public ComplexFlowValue(int size, Set<FlowValue> sources) {
        super(null, null, (FlowValue[]) null);
        this.size = size;
        this.sources = sources;
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
    public void mergeInputs(FlowValue[] newInputs) {
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public FlowValue mergeWith(FlowValue other) {
        if (this == other) {
            return this;
        }
        Set<FlowValue> newSources = new HashSet<>(sources);
        if (other instanceof ComplexFlowValue) {
            newSources.addAll(((ComplexFlowValue) other).sources);
        } else {
            newSources.add(other);
        }
        return new ComplexFlowValue(size, newSources);
    }

    @Override
    public Type getType() {
        return sources.stream().map(FlowValue::getType).reduce(TypeUtils::getCommonSupertype).get();
    }
}
