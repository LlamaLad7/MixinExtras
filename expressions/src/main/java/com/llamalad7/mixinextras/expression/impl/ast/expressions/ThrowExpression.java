package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Opcodes;

public class ThrowExpression extends Expression {
    public final Expression value;

    public ThrowExpression(ExpressionSource src, Expression value) {
        super(src);
        this.value = value;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        return node.getInsn().getOpcode() == Opcodes.ATHROW && inputsMatch(node, ctx, value);
    }
}
