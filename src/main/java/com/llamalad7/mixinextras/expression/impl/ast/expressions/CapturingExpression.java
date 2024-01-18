package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;

public class CapturingExpression implements SimpleExpression {
    public final Expression expression;

    public CapturingExpression(Expression expression) {
        this.expression = expression;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        boolean matches = expression.matches(node, ctx);
        if (matches) {
            expression.capture(node, ctx.getSink());
        }
        return matches;
    }
}
