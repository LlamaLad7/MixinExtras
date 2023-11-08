package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.google.gson.annotations.SerializedName;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedTypeName;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.List;

@SerializedTypeName("new")
public class InstantiationExpression implements Expression {
    @SerializedName("t")
    public final Identifier type;
    @SerializedName("args")
    public final List<Expression> arguments;

    public InstantiationExpression(Identifier type, List<Expression> arguments) {
        this.type = type;
        this.arguments = arguments;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, CaptureSink sink) {
        AbstractInsnNode insn = node.getInsn();
        if (insn.getOpcode() != Opcodes.NEW || !type.matches(pool, insn)) {
            return false;
        }
        for (Pair<FlowValue, Integer> next : node.getNext()) {
            if (next.getRight() != 0) continue;
            FlowValue nextValue = next.getLeft();
            AbstractInsnNode nextInsn = nextValue.getInsn();
            if (
                    nextInsn.getOpcode() == Opcodes.INVOKESPECIAL
                            && ((MethodInsnNode) nextInsn).name.equals("<init>")
                            && nextValue.getInput(0) == node
                            && inputsMatch(1, nextValue, pool, sink, arguments.toArray(new Expression[0]))
            ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void capture(FlowValue node, CaptureSink sink) {
        sink.accept(node.getInsn());
    }
}
