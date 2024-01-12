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

@SerializedExpressionId("is")
public class InstanceofExpression implements SimpleExpression {
    public final Expression expression;
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

    @Override
    public void write(ExpressionWriter writer) throws IOException {
        writer.writeExpression(expression);
        writer.writeIdentifier(type);
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return new InstanceofExpression(reader.readExpression(), reader.readIdentifier());
    }
}
