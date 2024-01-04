package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.objectweb.asm.Opcodes;

public class ThrowExpression implements Expression {
    private static final long serialVersionUID = 2812841095469875682L;

    public final Expression value;

    public ThrowExpression(Expression value) {
        this.value = value;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        return node.getInsn().getOpcode() == Opcodes.ATHROW && inputsMatch(node, pool, sink, value);
    }
}
