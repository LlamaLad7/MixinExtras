package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.utils.Decorations;
import org.objectweb.asm.Type;

public abstract class SimpleExpression extends Expression {
    public SimpleExpression(ExpressionSource src) {
        super(src);
    }

    @Override
    public void capture(FlowValue node, OutputSink sink) {
        if (node.getType() != Type.VOID_TYPE) {
            sink.decorate(node.getInsn(), Decorations.SIMPLE_EXPRESSION_TYPE, node.getType());
        }
        super.capture(node, sink);
    }
}
