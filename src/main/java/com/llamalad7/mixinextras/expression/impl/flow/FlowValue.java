package com.llamalad7.mixinextras.expression.impl.flow;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Value;

import java.util.*;

public class FlowValue implements Value {
    public static final FlowValue NULL = new FlowValue(Collections.emptyList(), Type.VOID_TYPE, null);
    private final List<FlowValue> parents;
    private final Set<Pair<FlowValue, Integer>> next = new HashSet<>();
    private final Type type;
    private final AbstractInsnNode insn;

    public FlowValue(FlowValue parent, Type type, AbstractInsnNode insn) {
        this(Collections.singletonList(parent), type, insn);
    }

    public FlowValue(List<FlowValue> parents, Type type, AbstractInsnNode insn) {
        this.parents = parents;
        this.type = type;
        this.insn = insn;
    }

    public void addChild(FlowValue value, int index) {
        next.add(Pair.of(value, index));
    }

    public void linkToParents() {
        for (int i = 0; i < parents.size(); i++) {
            parents.get(i).addChild(this, i);
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
        return parents.get(index);
    }
}
