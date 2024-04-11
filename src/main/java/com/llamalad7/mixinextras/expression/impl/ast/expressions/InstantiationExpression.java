package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.TypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.InstantiationInfo;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.utils.Decorations;

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
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        InstantiationInfo instantiation = node.getDecoration(Decorations.INSTANTIATION_INFO);
        if (instantiation == null) {
            return false;
        }
        return type.matches(ctx.pool, instantiation.type) && expressionsMatch(instantiation.args, arguments, ctx, ctx.allowIncompleteListInputs);
    }
}
