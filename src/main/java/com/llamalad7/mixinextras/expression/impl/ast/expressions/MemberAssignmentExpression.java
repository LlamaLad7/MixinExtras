package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionReader;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionWriter;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedExpressionId;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.io.IOException;

@SerializedExpressionId(".=")
public class MemberAssignmentExpression implements Expression {
    public final Expression receiver;
    public final Identifier name;
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

    @Override
    public void write(ExpressionWriter writer) throws IOException {
        writer.writeExpression(receiver);
        writer.writeIdentifier(name);
        writer.writeExpression(value);
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return new MemberAssignmentExpression(reader.readExpression(), reader.readIdentifier(), reader.readExpression());
    }
}
