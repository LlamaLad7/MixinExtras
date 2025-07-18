package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.MemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.MethodCallType;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;

import java.util.List;

public class SuperCallExpression extends SimpleExpression {
    public final MemberIdentifier name;
    public final List<Expression> arguments;

    public SuperCallExpression(ExpressionSource src, MemberIdentifier name, List<Expression> arguments) {
        super(src);
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        if (!MethodCallType.SUPER.matches(node)) {
            return false;
        }
        if (!name.matches(ctx.pool, node)) {
            return false;
        }
        return inputsMatch(node, ctx, ctx.allowIncompleteListInputs, arguments.toArray(new Expression[0]));
    }
}
