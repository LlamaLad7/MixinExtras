package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.objectweb.asm.Opcodes;

public class BooleanLiteralExpression implements SimpleExpression {
    public final boolean value;

    public BooleanLiteralExpression(boolean value) {
        this.value = value;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        if (value) {
            return node.getInsn().getOpcode() == Opcodes.ICONST_1;
        }
        return node.getInsn().getOpcode() == Opcodes.ICONST_0;
    }
}
