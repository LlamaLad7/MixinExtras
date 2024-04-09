package com.llamalad7.mixinextras.expression.impl.utils;

import com.llamalad7.mixinextras.expression.impl.ast.expressions.CapturingExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;

public class ExpressionUtil {
    public static Expression skipCapturesDown(Expression expr) {
        while (expr instanceof CapturingExpression) {
            expr = ((CapturingExpression) expr).expression;
        }
        return expr;
    }
}
