package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.google.gson.annotations.SerializedName;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedTypeName;

@SerializedTypeName("@")
public class CapturingExpression implements Expression {
    @SerializedName("ex")
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
}
