package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedTypeName;

@SerializedTypeName("?")
public class WildcardExpression implements Expression {
    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, CaptureSink sink) {
        return true;
    }
}