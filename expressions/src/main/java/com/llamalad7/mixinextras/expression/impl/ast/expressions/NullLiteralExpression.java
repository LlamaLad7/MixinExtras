package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Opcodes;

public class NullLiteralExpression extends SimpleExpression {
    public NullLiteralExpression(ExpressionSource src) {
        super(src);
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        return node.getInsn().getOpcode() == Opcodes.ACONST_NULL;
    }
}
