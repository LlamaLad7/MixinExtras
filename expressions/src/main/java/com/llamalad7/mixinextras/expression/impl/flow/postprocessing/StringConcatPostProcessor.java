package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.utils.FlowDecorations;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StringConcatPostProcessor implements FlowPostProcessor {
    private static final String STRING_BUILDER = Type.getInternalName(StringBuilder.class);

    @Override
    public void process(FlowValue node, OutputSink sink) {
        FlowValue firstAppend = getFirstAppend(node);
        if (firstAppend == null) {
            return;
        }
        List<FlowValue> appendCalls = new ArrayList<>();
        FlowValue currentAppend = firstAppend;
        FlowValue toStringCall;
        while (true) {
            appendCalls.add(currentAppend);
            Collection<Pair<FlowValue, Integer>> next = currentAppend.getNext();
            if (next.size() != 1) {
                return;
            }
            Pair<FlowValue, Integer> child = next.iterator().next();
            if (isAppendCall(child)) {
                currentAppend = child.getLeft();
                continue;
            }
            if (isToStringCall(child)) {
                toStringCall = child.getLeft();
                break;
            }
            return;
        }
        if (appendCalls.size() < 2) {
            // Not a concatenation.
            return;
        }
        decorateConcat(appendCalls, toStringCall);
    }

    public static void decorateConcat(List<FlowValue> appendCalls, FlowValue toStringCall) {
        FlowValue initialComponent = appendCalls.get(0).getInput(1);
        for (int i = 1; i < appendCalls.size() - 1; i++) {
            appendCalls.get(i).decorate(
                    FlowDecorations.STRING_CONCAT_INFO,
                    new StringConcatInfo(
                            i == 1,
                            false,
                            initialComponent,
                            toStringCall
                    )
            );
        }
        toStringCall.decorate(
                FlowDecorations.STRING_CONCAT_INFO,
                new StringConcatInfo(
                        appendCalls.size() == 2,
                        true,
                        initialComponent,
                        toStringCall
                )
        );
    }

    private FlowValue getFirstAppend(FlowValue node) {
        InstantiationInfo instantiation = node.getDecoration(FlowDecorations.INSTANTIATION_INFO);
        if (instantiation == null || !instantiation.type.getInternalName().equals(STRING_BUILDER)) {
            return null;
        }
        if (!isEmptyInit(instantiation.initCall)) {
            return null;
        }
        if (node.getNext().size() != 1) {
            return null;
        }
        Pair<FlowValue, Integer> firstAppend = node.getNext().iterator().next();
        if (isAppendCall(firstAppend)) {
            return firstAppend.getLeft();
        }
        return null;
    }

    private boolean isEmptyInit(FlowValue call) {
        return ((MethodInsnNode) call.getInsn()).desc.equals("()V");
    }

    private boolean isAppendCall(Pair<FlowValue, Integer> child) {
        if (child.getRight() != 0) {
            return false;
        }
        AbstractInsnNode insn = child.getLeft().getInsn();
        if (insn.getOpcode() != Opcodes.INVOKEVIRTUAL) {
            return false;
        }
        MethodInsnNode call = (MethodInsnNode) insn;
        return call.owner.equals(STRING_BUILDER) && call.name.equals("append") && Type.getArgumentTypes(call.desc).length == 1;
    }

    private boolean isToStringCall(Pair<FlowValue, Integer> child) {
        if (child.getRight() != 0) {
            return false;
        }
        AbstractInsnNode insn = child.getLeft().getInsn();
        if (insn.getOpcode() != Opcodes.INVOKEVIRTUAL) {
            return false;
        }
        MethodInsnNode call = (MethodInsnNode) insn;
        return call.owner.equals(STRING_BUILDER) && call.name.equals("toString");
    }
}
