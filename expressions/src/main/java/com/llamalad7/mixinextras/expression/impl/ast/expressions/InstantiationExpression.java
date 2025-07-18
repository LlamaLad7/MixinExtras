package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.TypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.InstantiationInfo;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.utils.FlowDecorations;

import java.util.List;

public class InstantiationExpression extends Expression {
    public final TypeIdentifier type;
    public final List<Expression> arguments;

    public InstantiationExpression(ExpressionSource src, TypeIdentifier type, List<Expression> arguments) {
        super(src);
        this.type = type;
        this.arguments = arguments;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        InstantiationInfo instantiation = node.getDecoration(FlowDecorations.INSTANTIATION_INFO);
        if (instantiation == null || !type.matches(ctx.pool, instantiation.type)) {
            return false;
        }
        return inputsMatch(node, ctx, ctx.allowIncompleteListInputs, arguments.toArray(new Expression[0]));
    }

    @Override
    protected void capture(FlowValue node, ExpressionContext ctx) {
        if (ctx.type == ExpressionContext.Type.REDIRECT) {
            throw new UnsupportedOperationException(
                    "Factory redirects are not supported with expressions! Either switch to @WrapOperation or use " +
                            "the standard NEW injection point."
            );
        }
        if (ctx.type == ExpressionContext.Type.MODIFY_ARG || ctx.type == ExpressionContext.Type.MODIFY_ARGS) {
            InstantiationInfo instantiation = node.getDecoration(FlowDecorations.INSTANTIATION_INFO);
            node = instantiation.initCall;
        }
        super.capture(node, ctx);
    }
}
