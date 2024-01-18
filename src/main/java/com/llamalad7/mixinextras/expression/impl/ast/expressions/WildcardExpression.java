package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;

public class WildcardExpression implements SimpleExpression {
    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        return true;
    }
}
