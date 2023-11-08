package com.llamalad7.mixinextras.utils.info;

import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ExtraMixinInfo {
    private final Map<String, Expression> parsedExpressions = new HashMap<>();

    public void offerExpression(String expr, Supplier<Expression> parser) {
        parsedExpressions.computeIfAbsent(expr, k -> parser.get());
    }

    public Expression getExpression(String expr) {
        return parsedExpressions.get(expr);
    }
}
