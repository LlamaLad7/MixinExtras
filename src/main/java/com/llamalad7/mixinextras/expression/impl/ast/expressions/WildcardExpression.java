package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;

public class WildcardExpression implements SimpleExpression {
    private static final long serialVersionUID = 2680435895042717848L;


    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        return true;
    }
}
