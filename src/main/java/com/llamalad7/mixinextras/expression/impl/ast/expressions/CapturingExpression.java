package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionReader;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionWriter;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedExpressionId;

import java.io.IOException;

@SerializedExpressionId("@")
public class CapturingExpression implements SimpleExpression {
    public final Expression expression;

    public CapturingExpression(Expression expression) {
        this.expression = expression;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        boolean matches = expression.matches(node, pool, sink);
        if (matches) {
            expression.capture(node, sink);
        }
        return matches;
    }

    @Override
    public void write(ExpressionWriter writer) throws IOException {
        writer.writeExpression(expression);
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return new CapturingExpression(reader.readExpression());
    }
}
