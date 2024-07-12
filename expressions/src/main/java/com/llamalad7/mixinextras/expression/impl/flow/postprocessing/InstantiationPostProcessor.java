package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.utils.FlowDecorations;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.util.Bytecode;

import java.util.stream.IntStream;

public class InstantiationPostProcessor implements FlowPostProcessor {
    @Override
    public void process(FlowValue node, OutputSink sink) {
        AbstractInsnNode insn = node.getInsn();
        if (insn.getOpcode() != Opcodes.NEW) {
            return;
        }
        Type newType = Type.getObjectType(((TypeInsnNode) insn).desc);
        FlowValue initCall = findInitCall(node);
        node.decorate(
                FlowDecorations.INSTANTIATION_INFO,
                new InstantiationInfo(
                        newType,
                        initCall
                )
        );
        sink.markAsSynthetic(initCall);
        node.setParents(IntStream.range(1, initCall.inputCount()).mapToObj(initCall::getInput).toArray(FlowValue[]::new));
    }

    private FlowValue findInitCall(FlowValue newNode) {
        for (Pair<FlowValue, Integer> next : newNode.getNext()) {
            if (next.getRight() != 0) continue;
            FlowValue nextValue = next.getLeft();
            AbstractInsnNode nextInsn = nextValue.getInsn();
            if (
                    nextInsn.getOpcode() == Opcodes.INVOKESPECIAL
                            && ((MethodInsnNode) nextInsn).name.equals("<init>")
                            && nextValue.getInput(0) == newNode
            ) {
                return nextValue;
            }
        }
        throw new IllegalStateException("Could not find <init> call for " + Bytecode.describeNode(newNode.getInsn()));
    }
}
