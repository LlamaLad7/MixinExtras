package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Opcodes;

public class BooleanLiteralExpression extends SimpleExpression {
    public final boolean value;

    public BooleanLiteralExpression(ExpressionSource src, boolean value) {
        super(src);
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
