package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.google.gson.annotations.SerializedName;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedTypeName;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

@SerializedTypeName("is")
public class InstanceofExpression implements Expression {
    @SerializedName("ex")
    public final Expression expression;
    @SerializedName("t")
    public final Identifier type;

    public InstanceofExpression(Expression expression, Identifier type) {
        this.expression = expression;
        this.type = type;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        AbstractInsnNode insn = node.getInsn();
        return insn.getOpcode() == Opcodes.INSTANCEOF && type.matches(pool, insn) && inputsMatch(node, pool, sink, expression);
    }
}
