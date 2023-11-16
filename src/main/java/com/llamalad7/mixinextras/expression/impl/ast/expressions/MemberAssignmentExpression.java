package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.google.gson.annotations.SerializedName;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedTypeName;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

@SerializedTypeName(".=")
public class MemberAssignmentExpression implements Expression {
    @SerializedName("ex")
    public final Expression receiver;
    @SerializedName("id")
    public final Identifier name;
    @SerializedName("v")
    public final Expression value;

    public MemberAssignmentExpression(Expression receiver, Identifier name, Expression value) {
        this.receiver = receiver;
        this.name = name;
        this.value = value;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        AbstractInsnNode insn = node.getInsn();
        return insn.getOpcode() == Opcodes.PUTFIELD
                && name.matches(pool, node.getInsn()) && inputsMatch(node, pool, sink, receiver, value);
    }
}
