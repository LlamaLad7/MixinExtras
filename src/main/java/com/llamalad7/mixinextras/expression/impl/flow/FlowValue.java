package com.llamalad7.mixinextras.expression.impl.flow;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Value;

import java.util.*;

public class FlowValue implements Value {
    private final int size;
    private final TypeSupplier typeComputer;
    private final TypeSupplier type;
    private final AbstractInsnNode insn;
    private final FlowValue[] parents;
    private final Set<Pair<FlowValue, Integer>> next = new HashSet<>(1);

    public FlowValue(int size, TypeSupplier typeComputer, AbstractInsnNode insn, FlowValue... parents) {
        this.size = size;
        this.typeComputer = typeComputer;
        this.type = typeComputer.memoize();
        this.insn = insn;
        this.parents = parents;
    }

    protected FlowValue(int size, AbstractInsnNode insn, FlowValue... parents) {
        this.size = size;
        this.typeComputer = this::computeType;
        this.type = typeComputer.memoize();
        this.insn = insn;
        this.parents = parents;
    }

    public void addChild(FlowValue value, int index) {
        next.add(Pair.of(value, index));
    }

    public void finish() {
        for (int i = 0; i < parents.length; i++) {
            parents[i].addChild(this, i);
        }
    }

    @Override
    public final int getSize() {
        return size;
    }

    protected Type computeType(FlowValue... inputs) {
        return typeComputer.get(inputs);
    }

    public final Type getType() {
        return type.get(parents);
    }

    public final Type getCurrentType() {
        return computeType(parents);
    }

    public AbstractInsnNode getInsn() {
        return insn;
    }

    public Collection<Pair<FlowValue, Integer>> getNext() {
        return next;
    }

    public FlowValue getInput(int index) {
        return parents[index];
    }

    public int inputCount() {
        return parents.length;
    }

    public Set<FlowValue> getComponents() {
        return Collections.singleton(this);
    }

    public FlowValue mergeWith(FlowValue other) {
        if (this == other) {
            return this;
        }
        Set<FlowValue> newComponents = new HashSet<>(getComponents());
        newComponents.addAll(other.getComponents());
        return new ComplexFlowValue(size, newComponents);
    }

    public void mergeInputs(FlowValue[] newInputs) {
        for (int i = 0; i < parents.length; i++) {
            parents[i] = parents[i].mergeWith(newInputs[i]);
        }
    }
}
