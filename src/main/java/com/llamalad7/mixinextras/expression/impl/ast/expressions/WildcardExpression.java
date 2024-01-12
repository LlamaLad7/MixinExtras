package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionReader;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionWriter;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedExpressionId;

import java.io.IOException;

@SerializedExpressionId("?")
public class WildcardExpression implements SimpleExpression {
    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        return true;
    }

    @Override
    public void write(ExpressionWriter writer) throws IOException {
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return new WildcardExpression();
    }
}
