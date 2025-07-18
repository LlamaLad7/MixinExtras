package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;

public class WildcardExpression extends SimpleExpression {
    public WildcardExpression(ExpressionSource src) {
        super(src);
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        return true;
    }
}
