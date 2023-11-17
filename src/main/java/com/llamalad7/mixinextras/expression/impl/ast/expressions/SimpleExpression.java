package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.utils.Decorations;
import org.objectweb.asm.Type;

public interface SimpleExpression extends Expression {
    @Override
    default void capture(FlowValue node, OutputSink sink) {
        if (node.getType() != Type.VOID_TYPE) {
            sink.decorate(node.getInsn(), Decorations.SIMPLE_EXPRESSION_TYPE, node.getType());
        }
        Expression.super.capture(node, sink);
    }
}
