package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Opcodes;

public class BooleanLiteralExpression implements SimpleExpression {
    public final boolean value;

    public BooleanLiteralExpression(boolean value) {
        this.value = value;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        if (value) {
            return node.getInsn().getOpcode() == Opcodes.ICONST_1;
        }
        return node.getInsn().getOpcode() == Opcodes.ICONST_0;
    }
}
