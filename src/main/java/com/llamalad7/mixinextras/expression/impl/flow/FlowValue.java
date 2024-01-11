package com.llamalad7.mixinextras.expression.impl.flow;

import com.llamalad7.mixinextras.utils.ASMUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Value;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class FlowValue implements Value {
    private final Type type;
    private final AbstractInsnNode insn;
    protected final FlowValue[] parents;
    private final Set<Pair<FlowValue, Integer>> next = new HashSet<>(1);

    public FlowValue(Type type, AbstractInsnNode insn, FlowValue... parents) {
        this.type = type;
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
    public int getSize() {
        return type.getSize();
    }

    public Type getType() {
        return type;
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

    public FlowValue mergeWith(FlowValue other) {
        if (this.equals(other)) {
            return this;
        }
        if (other instanceof ComplexFlowValue) {
            return other.mergeWith(this);
        }
        if (this.isTypeKnown() && other.isTypeKnown()) {
            return new DummyFlowValue(ASMUtils.getCommonSupertype(getType(), other.getType()));
        }
        return new ComplexFlowValue(getSize(), new HashSet<>(Arrays.asList(this, other)));
    }

    public void mergeInputs(FlowValue[] newInputs) {
        for (int i = 0; i < parents.length; i++) {
            parents[i] = parents[i].mergeWith(newInputs[i]);
        }
    }

    private boolean isTypeKnown() {
        return type != null;
    }

    public boolean isComplex() {
        return insn == null;
    }
}
