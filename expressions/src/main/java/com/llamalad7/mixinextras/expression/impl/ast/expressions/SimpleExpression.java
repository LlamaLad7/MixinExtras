package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.MatchResult;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionDecorations;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import org.objectweb.asm.Type;

public abstract class SimpleExpression extends Expression {
    public SimpleExpression(ExpressionSource src) {
        super(src);
    }

    @Override
    public MatchResult capture(FlowValue node, ExpressionContext ctx, MatchResult result) {
        Type type = node.getType();
        if (type.equals(ExpressionASMUtils.BOTTOM_TYPE)) {
            type = ExpressionASMUtils.OBJECT_TYPE;
        }
        if (!type.equals(Type.VOID_TYPE)) {
            result = result.thenDecorate(node.getInsn(), ExpressionDecorations.SIMPLE_EXPRESSION_TYPE, type);
        }
        return super.capture(node, ctx, result);
    }
}
