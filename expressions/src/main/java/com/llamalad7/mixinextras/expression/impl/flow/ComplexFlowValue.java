package com.llamalad7.mixinextras.expression.impl.flow;

import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.HashSet;
import java.util.Set;

public class ComplexFlowValue extends FlowValue {
    private final int size;
    private final Set<FlowValue> sources;
    private final FlowContext context;

    public ComplexFlowValue(int size, Set<FlowValue> sources, FlowContext context) {
        super(null, null, (FlowValue[]) null);
        this.size = size;
        this.sources = sources;
        this.context = context;
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
        return 0;
    }

    @Override
    public void mergeInputs(FlowValue[] newInputs, FlowContext ctx) {
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public FlowValue mergeWith(FlowValue other, FlowContext ctx) {
        if (this == other) {
            return this;
        }
        Set<FlowValue> newSources = new HashSet<>(sources);
        if (other instanceof ComplexFlowValue) {
            newSources.addAll(((ComplexFlowValue) other).sources);
        } else {
            newSources.add(other);
        }
        return new ComplexFlowValue(size, newSources, ctx);
    }

    @Override
    public Type getType() {
        return sources.stream().map(FlowValue::getType).reduce((type1, type2) -> ExpressionASMUtils.getCommonSupertype(context, type1, type2)).get();
    }
}
