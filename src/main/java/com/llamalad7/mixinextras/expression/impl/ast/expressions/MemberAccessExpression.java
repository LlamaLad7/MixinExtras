package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.google.gson.annotations.SerializedName;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedTypeName;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

@SerializedTypeName(".")
public class MemberAccessExpression implements Expression {
    @SerializedName("ex")
    public final Expression receiver;
    @SerializedName("id")
    public final Identifier name;

    public MemberAccessExpression(Expression receiver, Identifier name) {
        this.receiver = receiver;
        this.name = name;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, CaptureSink sink) {
        AbstractInsnNode insn = node.getInsn();
        return insn.getOpcode() == Opcodes.GETFIELD && name.matches(pool, insn) && inputsMatch(node, pool, sink, receiver);
    }
}