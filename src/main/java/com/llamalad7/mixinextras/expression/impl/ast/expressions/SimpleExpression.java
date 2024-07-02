package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionDecorations;
import com.llamalad7.mixinextras.utils.TypeUtils;
import org.objectweb.asm.Type;

public abstract class SimpleExpression extends Expression {
    public SimpleExpression(ExpressionSource src) {
        super(src);
    }

    @Override
    public void capture(FlowValue node, ExpressionContext ctx) {
        Type type = node.getType();
        if (type.equals(TypeUtils.BOTTOM_TYPE)) {
            type = TypeUtils.OBJECT_TYPE;
        }
        if (!type.equals(Type.VOID_TYPE)) {
            ctx.decorate(node.getInsn(), ExpressionDecorations.SIMPLE_EXPRESSION_TYPE, type);
        }
        super.capture(node, ctx);
    }
}
