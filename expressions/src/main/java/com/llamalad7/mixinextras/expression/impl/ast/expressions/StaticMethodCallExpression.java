package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.MatchResult;
import com.llamalad7.mixinextras.expression.impl.ast.Argument;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.MemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.MethodCallType;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;

import java.util.List;

public class StaticMethodCallExpression extends SimpleExpression {
    public final MemberIdentifier name;
    public final List<Argument> arguments;

    public StaticMethodCallExpression(ExpressionSource src, MemberIdentifier name, List<Argument> arguments) {
        super(src);
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    protected MatchResult matchImpl(FlowValue node, ExpressionContext ctx) {
        return MethodCallType.STATIC.matches(node)
                && name.matches(ctx.pool, node)
                ? matchArguments(node, ctx, arguments)
                : MatchResult.FAILURE;
    }
}
