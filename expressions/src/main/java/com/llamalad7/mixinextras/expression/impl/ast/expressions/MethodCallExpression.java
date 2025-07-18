package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.MemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.MethodCallType;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

public class MethodCallExpression extends SimpleExpression {
    public final Expression receiver;
    public final MemberIdentifier name;
    public final List<Expression> arguments;

    public MethodCallExpression(ExpressionSource src, Expression receiver, MemberIdentifier name, List<Expression> arguments) {
        super(src);
        this.receiver = receiver;
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        if (!MethodCallType.NORMAL.matches(node)) {
            return false;
        }
        if (!name.matches(ctx.pool, node)) {
            return false;
        }
        Expression[] inputs = ArrayUtils.add(arguments.toArray(new Expression[0]), 0, receiver);
        return inputsMatch(node, ctx, ctx.allowIncompleteListInputs, inputs);
    }
}
