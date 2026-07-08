package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.MatchResult;
import com.llamalad7.mixinextras.expression.impl.ast.Argument;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.MemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.MethodCallType;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;

import java.util.ArrayList;
import java.util.List;

public class MethodCallExpression extends SimpleExpression {
    public final Expression receiver;
    public final MemberIdentifier name;
    public final List<Argument> arguments;

    public MethodCallExpression(ExpressionSource src, Expression receiver, MemberIdentifier name, List<Argument> arguments) {
        super(src);
        this.receiver = receiver;
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    protected MatchResult matchImpl(FlowValue node, ExpressionContext ctx) {
        if (!MethodCallType.NORMAL.matches(node)) {
            return MatchResult.FAILURE;
        }
        if (!name.matches(ctx.pool, node)) {
            return MatchResult.FAILURE;
        }
        List<Argument> arguments = new ArrayList<>(this.arguments);
        arguments.add(0, new Argument.Concrete(receiver));
        return matchArguments(node, ctx, arguments);
    }
}
