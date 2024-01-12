package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionReader;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionWriter;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedExpressionId;
import org.objectweb.asm.Opcodes;

import java.io.IOException;

@SerializedExpressionId("throw")
public class ThrowExpression implements Expression {
    public final Expression value;

    public ThrowExpression(Expression value) {
        this.value = value;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        return node.getInsn().getOpcode() == Opcodes.ATHROW && inputsMatch(node, pool, sink, value);
    }

    @Override
    public void write(ExpressionWriter writer) throws IOException {
        writer.writeExpression(value);
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return new ThrowExpression(reader.readExpression());
    }
}
