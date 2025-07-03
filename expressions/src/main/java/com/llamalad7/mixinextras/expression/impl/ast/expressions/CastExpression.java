package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.TypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionDecorations;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class CastExpression extends SimpleExpression {
    public final TypeIdentifier type;
    public final Expression expression;

    public CastExpression(ExpressionSource src, TypeIdentifier type, Expression expression) {
        super(src);
        this.type = type;
        this.expression = expression;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        Type castType = ExpressionASMUtils.getCastType(node.getInsn());
        return castType != null && type.matches(ctx.pool, castType) && inputsMatch(node, ctx, expression);
    }

    @Override
    public void capture(FlowValue node, ExpressionContext ctx) {
        if (node.getInsn().getOpcode() == Opcodes.CHECKCAST) {
            ctx.decorate(node.getInsn(), ExpressionDecorations.SIMPLE_OPERATION_ARGS, new Type[]{ExpressionASMUtils.OBJECT_TYPE});
            ctx.decorate(node.getInsn(), ExpressionDecorations.SIMPLE_OPERATION_RETURN_TYPE, node.getType());
            ctx.decorate(node.getInsn(), ExpressionDecorations.SIMPLE_OPERATION_PARAM_NAMES, new String[]{"object"});
        }
        super.capture(node, ctx);
    }
}
