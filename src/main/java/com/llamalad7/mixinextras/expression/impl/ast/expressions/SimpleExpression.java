package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.utils.Decorations;
import com.llamalad7.mixinextras.utils.TypeUtils;
import org.objectweb.asm.Type;

public abstract class SimpleExpression extends Expression {
    public SimpleExpression(ExpressionSource src) {
        super(src);
    }

    @Override
    public void capture(FlowValue node, OutputSink sink) {
        Type type = node.getType();
        if (type.equals(TypeUtils.BOTTOM_TYPE)) {
            type = TypeUtils.OBJECT_TYPE;
        }
        if (!type.equals(Type.VOID_TYPE)) {
            sink.decorate(node.getInsn(), Decorations.SIMPLE_EXPRESSION_TYPE, type);
        }
        super.capture(node, sink);
    }
}
